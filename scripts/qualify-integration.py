#!/usr/bin/env python3
"""Real loopback TLS API tests using Android's Kotlin client and the Go agent.
All identities/jobs/traffic are temporary local test data. No external provider claim.
"""
import base64,datetime,hashlib,http.server,json,os,pathlib,select,signal,socket,ssl,subprocess,sys,tempfile,threading,time,uuid
ROOT=pathlib.Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import ec
import ipaddress
from proof_jobs import create,validate,verify_result
from verify import load,verify_node
checks=[]
def record(name):checks.append({'id':name,'status':'PASS'});print('PASS:',name,flush=True)
def freeport():
 with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
received=[]
class Receiver(http.server.BaseHTTPRequestHandler):
 def do_POST(self):
  n=int(self.headers['Content-Length']);b=self.rfile.read(n);received.append(len(b));self.send_response(200);self.end_headers();self.wfile.write(b'accepted-local-payload')
 def log_message(self,*args):pass

def main():
 with tempfile.TemporaryDirectory(prefix='deproof-integration-') as temp:
  root=pathlib.Path(temp);key=ec.generate_private_key(ec.SECP256R1());subject=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'local known-byte test receiver')]);now=datetime.datetime.now(datetime.timezone.utc)
  cert=x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=1)).add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]),False).add_extension(x509.BasicConstraints(ca=True,path_length=0),True).sign(key,hashes.SHA256())
  certpath=root/'receiver.pem';keypath=root/'receiver.key';certpath.write_bytes(cert.public_bytes(serialization.Encoding.PEM));keypath.write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
  receiver=http.server.ThreadingHTTPServer(('127.0.0.1',0),Receiver);tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);tls.load_cert_chain(certpath,keypath);receiver.socket=tls.wrap_socket(receiver.socket,server_side=True);threading.Thread(target=receiver.serve_forever,daemon=True).start()
  job=create(root/'job');jobpath=root/'job/job.json';owner=root/'job/owner-public.key';state=root/'state';env=dict(os.environ,SSL_CERT_FILE=str(certpath));port=freeport();process=None
  seed=base64.b64encode(os.urandom(32)).decode();base={'seed':seed,'endpoint':f'https://127.0.0.1:{port}'}
  cp=(ROOT/'evidence/qualification/jvm-classpath.txt').read_text()
  def start(scopes,hosting=False):
   nonlocal process
   process=subprocess.Popen([str(ROOT/'node-agent/build/deproof-node'),'--state',str(state),'--listen',f'127.0.0.1:{port}','--contribution-endpoint',f'https://127.0.0.1:{receiver.server_port}/receive','--contribution-ca',str(certpath),'--pair-scopes',','.join(scopes),'--proof-tool',str(ROOT/'tools/proof_jobs.py'),'--proof-job',str(jobpath),'--proof-owner',str(owner)]+(['--hosting-tool',str(ROOT/'tools/local_hosting.py'),'--hosting-evidence',str(ROOT/'evidence/qualification/hosting-isolation.json')] if hosting else []),stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,env=env)
   info={};deadline=time.monotonic()+10
   while time.monotonic()<deadline:
    line=process.stdout.readline()
    if not line:raise AssertionError('Node exited before listening: '+process.stderr.read())
    if line.startswith('Node fingerprint: '):info['fingerprint']=line.strip().split(': ',1)[1]
    if line.startswith('Pairing challenge (2 minutes): '):info['challenge']=json.loads(line.split(': ',1)[1])
    if line.startswith('Single-use pairing code: '):info['code']=line.strip().split(': ',1)[1]
    if line.startswith('TLS certificate SHA-256: '):info['pin']=line.strip().split(': ',1)[1]
    if line.startswith('Loopback-only TLS listener: '):break
   base.update(info)
   # No pairing code/private seed is logged or exported.
   base['scopes']=scopes
   for _ in range(50):
    try:
     with socket.create_connection(('127.0.0.1',port),.1):break
    except OSError:time.sleep(.05)
  def stop():
   if process and process.poll() is None:process.terminate();process.wait(timeout=5)
  def request(action=None,params=None,expected=None,operation=None,overrides=None):
   value=dict(base,mode='command' if action else 'pair',params=params or {},action=action,operationId=operation or str(uuid.uuid4()))
   if overrides:value.update(overrides)
   r=subprocess.run(['java','-cp',cp,'com.example.probe.NodeApiProbeKt'],input=json.dumps(value),text=True,capture_output=True,timeout=75)
   if expected:
    assert r.returncode!=0 and expected in r.stderr,(expected,r.stderr);return None
   assert r.returncode==0,r.stderr
   return json.loads(r.stdout)
  try:
   start(['READ_NODE']);base['sessionId']=request()['sessionId'];assert request('observe')['bandwidth']['enabled'] is False
   request('transfer',{'bytes':'1'},expected='SCOPE_DENIED');record('read_only_scope_cannot_transfer')
   request('observe',expected='TLS_PIN_MISMATCH',overrides={'pin':'0'*64});record('wrong_tls_certificate_pin_denied')
   request('revoke');request('observe',expected='SESSION_REVOKED_OR_UNKNOWN');record('api_revocation_denies_subsequent_commands')
   stop();start(['READ_NODE','MANAGE_SERVICE','SHARE_BANDWIDTH','RUN_PROOF_JOB']);base['sessionId']=request()['sessionId']
   request('startService',expected='RUNTIME_ISOLATION_UNQUALIFIED');record('unqualified_hosting_refuses_start')
   operation=str(uuid.uuid4());request('observe',operation=operation);request('observe',operation=operation,expected='REPLAYED_OPERATION');record('operation_replay_denied')
   receiver_url=f'https://127.0.0.1:{receiver.server_port}/receive'
   consent={'endpoint':receiver_url,'capBytes':'4096','bytesPerSecond':'1048576','deadline':(now+datetime.timedelta(minutes=5)).isoformat().replace('+00:00','Z'),'explicitConsent':False}
   request('consent',consent,expected='CONSENT_PROFILE_DENIED');record('bandwidth_requires_explicit_consent')
   consent['explicitConsent']=True;request('consent',consent)
   usage=request('transfer',{'bytes':'1024'});assert verify_node(usage)['integrity']=='PASS';event=load(base64.b64decode(usage['payloadBase64']));assert event['measuredBytes']=='1024' and received[-1]==1024
   record('real_tls_transfer_sender_receiver_counters_match')
   request('transfer',{'bytes':'4096'},expected='QUOTA_OR_CONSENT_DENIED');record('quota_enforced_before_network_io')
   consent['bytesPerSecond']='1';request('consent',consent)
   outcome=[]
   def flow():
    try:request('transfer',{'bytes':'1024'})
    except Exception as e:outcome.append(type(e).__name__)
   thread=threading.Thread(target=flow);thread.start()
   for _ in range(10):
    status=request('observe')
    if status['activeOperations']:break
    time.sleep(.1)
   assert status['activeOperations'],'No actual active flow'
   request('stop');thread.join(10);assert not thread.is_alive();status=request('observe');assert not status['activeOperations'] and status['bandwidth']['enabled'] is False
   record('stop_cancels_real_flow_and_disables_consent')
   discovered=request('discoverProofJobs');assert discovered['jobId']==job['id'];record('signed_owner_job_discovered')
   signedjob=jobpath.read_bytes();modified=json.loads(signedjob);modified['signatureBase64']=base64.b64encode(bytes(64)).decode();jobpath.write_text(json.dumps(modified))
   request('discoverProofJobs',expected='PROOF_JOB_VALIDATION_OR_EXECUTION_FAILED');jobpath.write_bytes(signedjob);record('forged_job_signature_denied')
   receipt=request('proof',{'jobId':job['id'],'explicitConsent':True});payload=load(base64.b64decode(receipt['payloadBase64']));assert payload['verificationObservation']['status']=='VALID';assert verify_node(receipt)['integrity']=='PASS';record('android_client_to_agent_to_prover_to_verifier_signed_receipt')
   proofs=list(state.glob('proof-*/result'));assert len(proofs)==1;result=proofs[0]
   wrong=dict(job,publicInputs={'y':'36'})
   try:verify_result(result,wrong);raise AssertionError('Wrong input passed')
   except subprocess.CalledProcessError:pass
   record('wrong_public_input_rejected_by_independent_verifier')
   proofpath=result/'proof.bin';original=proofpath.read_bytes();proofpath.write_bytes(original[:-1]+bytes([original[-1]^1]))
   try:verify_result(result,job);raise AssertionError('Tampered proof passed')
   except ValueError:pass
   proofpath.write_bytes(original);record('tampered_result_digest_rejected')
   vkpath=result/'verification-key.bin';originalkey=vkpath.read_bytes();vkpath.write_bytes(originalkey+b'!')
   try:verify_result(result,job);raise AssertionError('Wrong key passed')
   except ValueError:pass
   vkpath.write_bytes(originalkey);record('pinned_verification_key_mismatch_rejected')
   altered=dict(receipt);altered['payloadBase64']=base64.b64encode(base64.b64decode(receipt['payloadBase64'])+b' ').decode()
   try:verify_node(altered);raise AssertionError('Tampered receipt passed')
   except Exception as e:
    if isinstance(e,AssertionError):raise
   record('signed_contribution_tamper_rejected')
   os.kill(process.pid,signal.SIGUSR1);time.sleep(.1);request('observe',expected='SESSION_REVOKED_OR_UNKNOWN');record('owner_local_revocation_works_without_phone')
   before=json.loads((state/'state.json').read_text())['consent']['reservedBytes'];stop();start(['READ_NODE']);base['sessionId']=request()['sessionId'];status=request('observe');assert status['bandwidth']['reservedBytes']==before and status['bandwidth']['enabled'] is False;record('restart_retains_charged_reservations_and_disables_sharing')
   if os.environ.get('DEPROOF_TEST_HOSTING')=='1':
    stop();start(['READ_NODE','MANAGE_SERVICE'],hosting=True);base['sessionId']=request()['sessionId']
    observed=request('observe')['hosting'];assert isinstance(observed,dict),observed
    digest=observed['profileDigest']
    request('startService',{'profileDigest':'0'*64,'explicitConsent':True},expected='SERVICE_RUNTIME_OR_PROFILE_FAILED');record('changed_service_profile_denied')
    service=request('startService',{'profileDigest':digest,'explicitConsent':True});assert service['observedState']=='RUNNING' and service['healthy'] is True
    cid=service['containerId'];record('authenticated_allowlisted_service_actually_starts_and_is_healthy')
    stop();start(['READ_NODE','MANAGE_SERVICE'],hosting=True);base['sessionId']=request()['sessionId'];recovered=request('observe')['hosting'];assert recovered['containerId']==cid and recovered['observedState']=='EXITED';record('restart_revocation_stops_and_observes_existing_container')
    stopped=request('stopService');assert stopped['observedState']=='EXITED' and stopped['healthy'] is None;record('service_stop_observes_termination')
    observed=request('observe')['hosting'];request('startService',{'profileDigest':observed['profileDigest'],'explicitConsent':True});request('revoke')
    external=subprocess.run(['docker','inspect',cid,'--format','{{.State.Status}}'],capture_output=True,text=True,check=True);assert external.stdout.strip()=='exited';record('session_revocation_stops_actual_owned_service')
    request('observe',expected='SESSION_REVOKED_OR_UNKNOWN')
    subprocess.run(['docker','rm',cid],check=True,capture_output=True)

   out=ROOT/'evidence/qualification/node-integration.json';out.write_text(json.dumps({'schema':'deproof-gate-v1','status':'PASS','checks':checks,'limitations':['Real loopback topology only; no Android device/Keystore test.','Local educational trusted setup; external protocol membership/rewards and runtime isolation unqualified.']},indent=2)+'\n')
  finally:stop();receiver.shutdown();receiver.server_close()
if __name__=='__main__':main()
