#!/usr/bin/env python3
"""Observe the actual restricted development containers; no device/provider assurance."""
import base64,datetime,hashlib,json,pathlib,ssl,subprocess,tempfile,time,urllib.request,urllib.error,uuid,os,sys
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding,PublicFormat
ROOT=pathlib.Path(__file__).resolve().parents[1];TEST_ENV=dict(os.environ,NODE_PORT='19843',WEB_PORT='18088',NODE_PAIR_SCOPES='READ_NODE',NODE_CONTRIBUTION_ENDPOINT='',NODE_PROOF_JOB='',NODE_PROOF_OWNER='');OUT=ROOT/'evidence/docker';OUT.mkdir(parents=True,exist_ok=True)
checks=[]
class EnvironmentBlocked(RuntimeError):pass
def cli(*args):return subprocess.run(['docker','compose','-p','deproof-qualification',*args],env=TEST_ENV,cwd=ROOT,capture_output=True,text=True,timeout=180)
def check(name,ok):
 checks.append({'id':name,'status':'PASS' if ok else 'FAIL'});print(checks[-1]['status']+': '+name,flush=True)
 if not ok:raise AssertionError(name)
def main():
 r=cli('config','--format','json');check('compose_config_valid',r.returncode==0);cfg=json.loads(r.stdout)
 for name in ['node-agent','prover-worker','web']:
  s=cfg['services'][name];check(name+'_restricted_config',s['read_only'] and 'ALL' in s['cap_drop'] and int(s['pids_limit'])>0 and int(s['mem_limit'])>0 and not any('docker.sock' in str(m) for m in s.get('volumes',[])))
 r=cli('up','-d','--wait','node-agent','prover-worker','web')
 if r.returncode:
  print(r.stderr,file=sys.stderr)
  # Inspect privately; node logs can contain single-use pairing secrets.
  logs=cli('logs','--no-log-prefix','node-agent').stdout
  if 'no space left on device' in logs:
   checks.append({'id':'actual_services_started','status':'BLOCKED','reason':'Docker state storage exhausted; no space left on device.'})
   raise EnvironmentBlocked('Docker state storage exhausted')
 check('actual_services_started',r.returncode==0)
 ids={}
 for name in ['node-agent','prover-worker','web']:
  r=cli('ps','-q',name);cid=r.stdout.strip();check(name+'_has_container',r.returncode==0 and bool(cid));ids[name]=cid
  p=subprocess.run(['docker','inspect',cid],capture_output=True,text=True,check=True);data=json.loads(p.stdout)[0];h=data['HostConfig']
  check(name+'_observed_nonroot_restrictions',data['Config']['User'] not in ['','0','0:0','root'] and h['ReadonlyRootfs'] and h['CapDrop']==['ALL'] and 'no-new-privileges:true' in h['SecurityOpt'] and h['Memory']>0 and h['PidsLimit']>0 and h['NanoCpus']>0 and not any(m.get('Source','').endswith('docker.sock') for m in data['Mounts']))
  for bindings in (h.get('PortBindings') or {}).values():check(name+'_host_port_loopback',all(b['HostIp']=='127.0.0.1' for b in bindings))
 with urllib.request.urlopen('http://127.0.0.1:18088/',timeout=10) as response:body=response.read().decode()
 check('real_website_branding', 'CodesbyFebin' in body and 'DEPR' in body)
 logs=cli('logs','--no-log-prefix','node-agent').stdout;info={}
 for line in logs.splitlines():
  if line.startswith('Pairing challenge (2 minutes): '):info['challenge']=json.loads(line.split(': ',1)[1])
  elif line.startswith('Single-use pairing code: '):info['code']=line.split(': ',1)[1].strip()
  elif line.startswith('Node fingerprint: '):info['fingerprint']=line.split(': ',1)[1].strip()
  elif line.startswith('TLS certificate SHA-256: '):info['pin']=line.split(': ',1)[1].strip()
 check('owner_pairing_information_available',len(info)==4)
 with tempfile.TemporaryDirectory(prefix='deproof-docker-') as tmp:
  cert=pathlib.Path(tmp)/'tls.pem';r=cli('cp','node-agent:/state/tls.pem',str(cert));check('owner_certificate_exported',r.returncode==0)
  der=ssl.PEM_cert_to_DER_cert(cert.read_text());check('owner_certificate_pin_matches',hashlib.sha256(der).hexdigest()==info['pin'])
  context=ssl.create_default_context(cafile=str(cert));context.minimum_version=ssl.TLSVersion.TLSv1_3
  key=Ed25519PrivateKey.generate();enc=lambda b:base64.b64encode(b).decode();pub=enc(key.public_key().public_bytes(Encoding.Raw,PublicFormat.Raw))
  def send(path,data):
   req=urllib.request.Request('https://127.0.0.1:19843'+path,json.dumps(data,separators=(',',':')).encode(),{'Content-Type':'application/json'},method='POST')
   with urllib.request.urlopen(req,context=context,timeout=15) as response:return json.loads(response.read(65536))
  c=info['challenge'];raw=json.dumps({'domain':'deproof-pair-v1','challenge':{k:c[k] for k in ('id','nonce','fingerprint','expiresAt')},'publicKey':pub,'scopes':['READ_NODE']},separators=(',',':')).encode()
  paired=send('/pair',{'challengeId':c['id'],'code':info['code'],'publicKey':pub,'signatureBase64':enc(key.sign(raw)),'approvedFingerprint':info['fingerprint'],'scopes':['READ_NODE']});check('authenticated_pairing_over_verified_tls',bool(paired['sessionId']))
  def command(action,operation=None):
   payload=json.dumps({'sessionId':paired['sessionId'],'operationId':operation or str(uuid.uuid4()),'deadline':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=45)).isoformat().replace('+00:00','Z'),'policy':'node-policy-v1','action':action,'params':{}},separators=(',',':')).encode()
   return send('/command',{'payloadBase64':enc(payload),'signatureBase64':enc(key.sign(payload))})
  operation=str(uuid.uuid4());observed=command('observe',operation);check('sharing_observed_disabled',observed['bandwidth']['enabled'] is False)
  for action,op in [('observe',operation),('transfer',None)]:
   try:command(action,op);denied=False
   except urllib.error.HTTPError as e:denied=json.loads(e.read(65536)).get('error') in ['REPLAYED_OPERATION','SCOPE_DENIED','SESSION_REVOKED_OR_UNKNOWN']
   check(action+'_replay_or_scope_denied',denied)
  command('revoke')
  try:command('observe');denied=False
  except urllib.error.HTTPError as e:denied=json.loads(e.read(65536)).get('error') in ['REPLAYED_OPERATION','SCOPE_DENIED','SESSION_REVOKED_OR_UNKNOWN']
  check('revoked_session_denied',denied)
  # Restart creates a fresh challenge and preserves the node identity/state; never deletes a volume.
  check('node_restart_success',cli('restart','node-agent').returncode==0)
  time.sleep(2);cert2=pathlib.Path(tmp)/'tls2.pem';cli('cp','node-agent:/state/tls.pem',str(cert2))
  check('persistent_tls_identity_retained',cert.read_bytes()==cert2.read_bytes())
 return ids
status='FAIL';details={};failure=None
try:details=main();status='PASS'
except EnvironmentBlocked as e:status='BLOCKED';failure=str(e);print('BLOCKED: '+failure,file=sys.stderr)
except Exception as e:failure=type(e).__name__+': '+str(e);print('FAIL: '+failure,file=sys.stderr)
finally:cli('down')
report={'schema':'deproof-container-integration-v1','status':status,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checks':checks,'containers':details,'failure':failure,'limitations':['Owner-local Docker Linux/amd64 only. No physical-device, wallet, hardware-key, production proof or external provider qualification.','Separate deproof-qualification project on localhost ports 19843/18088; fixtures are stopped and persistent volumes retained. Pairing secrets are not exported to evidence.']}
(OUT/'integration-report.json').write_text(json.dumps(report,indent=2)+'\n')
if status!='PASS':sys.exit(1)
