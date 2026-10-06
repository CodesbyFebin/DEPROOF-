#!/usr/bin/env python3
"""Owner-authenticated local cubic jobs. Separate gnark producer/verifier processes.
Educational local setup, bounded timeout; OS/container resource isolation is NOT qualified.
"""
import argparse,base64,datetime,hashlib,json,os,pathlib,subprocess,uuid
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey,Ed25519PublicKey
from cryptography.hazmat.primitives.serialization import Encoding,PublicFormat,PrivateFormat,NoEncryption
from verify import canonical,load,digest,b64,require
ROOT=pathlib.Path(__file__).resolve().parents[1]
def atomic_json(path,value):
 path=pathlib.Path(path);temporary=path.with_suffix(path.suffix+'.tmp')
 with temporary.open('w') as f:json.dump(value,f);f.flush();os.fsync(f.fileno())
 os.replace(temporary,path)
 fd=os.open(path.parent,os.O_RDONLY)
 try:os.fsync(fd)
 finally:os.close(fd)
def now():return datetime.datetime.now(datetime.timezone.utc)
def timestamp():return now().isoformat().replace('+00:00','Z')
def read_bounded(path,limit=65536):
 p=pathlib.Path(path);require(p.is_file() and not p.is_symlink() and p.stat().st_size<=limit,'JOB_FILE_DENIED');return p.read_bytes()
def invoke(args,timeout=45):
 return subprocess.run(list(map(str,args)),check=True,capture_output=True,timeout=timeout)
def create(directory):
 directory=pathlib.Path(directory);directory.mkdir(mode=0o700,parents=True,exist_ok=False)
 key=Ed25519PrivateKey.generate();public=key.public_key().public_bytes(Encoding.Raw,PublicFormat.Raw)
 setup=directory/'setup';invoke([ROOT/'prover-worker/build/prove',setup])
 job={'schema':'deproof-proof-job-v1','id':str(uuid.uuid4()),'backend':'gnark-v0.14.0','scheme':'Groth16/BN254','circuitDigest':digest((setup/'circuit.r1cs').read_bytes()),'verificationKeyDigest':digest((setup/'verification-key.bin').read_bytes()),'inputCommitment':digest(canonical({'x':'3','y':'35'})),'publicInputs':{'y':'35'},'maxWitnessBytes':'32','deadline':(now()+datetime.timedelta(minutes=30)).isoformat().replace('+00:00','Z'),'source':'OWNER_LOCAL_CUBIC_SAMPLE','compensation':None}
 job['resourceProfile']={'circuit':'owner-local-cubic','circuitVersion':job['circuitDigest'],'minMemoryMiB':'256','minCpuCores':'1'}
 raw=canonical(job);signed={'schema':'deproof-signed-job-v1','payloadBase64':base64.b64encode(raw).decode(),'signatureBase64':base64.b64encode(key.sign(raw)).decode(),'publicKeyBase64':base64.b64encode(public).decode()}
 (directory/'owner-public.key').write_text(base64.b64encode(public).decode());(directory/'job.json').write_text(json.dumps(signed,indent=2))
 # No owner private key persisted: the example issuer cannot silently replace a signed job.
 return job
FIELDS={'schema','id','backend','scheme','circuitDigest','verificationKeyDigest','inputCommitment','publicInputs','maxWitnessBytes','deadline','source','compensation'}
def validate(job_path,owner_path):
 signed=load(read_bounded(job_path));require(set(signed)=={'schema','payloadBase64','signatureBase64','publicKeyBase64'} and signed['schema']=='deproof-signed-job-v1','JOB_ENVELOPE')
 public=b64(read_bounded(owner_path,256).decode().strip());require(b64(signed['publicKeyBase64'])==public,'JOB_OWNER_MISMATCH')
 raw=b64(signed['payloadBase64']);require(len(raw)<=32768,'JOB_TOO_LARGE');Ed25519PublicKey.from_public_bytes(public).verify(b64(signed['signatureBase64']),raw)
 job=load(raw);require(set(job) in (FIELDS,FIELDS|{'resourceProfile'}) and canonical(job)==raw,'JOB_FIELDS_OR_CANONICALIZATION')
 if 'resourceProfile' in job:
  require(job['resourceProfile']=={'circuit':'owner-local-cubic','circuitVersion':job['circuitDigest'],'minMemoryMiB':'256','minCpuCores':'1'},'RESOURCE_PROFILE_DENIED')
 require(job['schema']=='deproof-proof-job-v1' and job['backend']=='gnark-v0.14.0' and job['scheme']=='Groth16/BN254','BACKEND_MISMATCH')
 require(job['publicInputs']=={'y':'35'} and job['inputCommitment']==digest(canonical({'x':'3','y':'35'})) and job['maxWitnessBytes']=='32' and job['compensation'] is None,'INPUT_PROFILE_DENIED')
 require(datetime.datetime.fromisoformat(job['deadline'].replace('Z','+00:00'))>now(),'JOB_EXPIRED')
 setup=pathlib.Path(job_path).resolve().parent/'setup'
 for name,field in [('circuit.r1cs','circuitDigest'),('verification-key.bin','verificationKeyDigest')]:require(digest(read_bounded(setup/name,16*1024*1024))==job[field],'PINNED_SETUP_MISMATCH')
 return job,setup

def verify_result(directory,job):
 directory=pathlib.Path(directory)
 require(digest(read_bounded(directory/'circuit.r1cs',16*1024*1024))==job['circuitDigest'],'CIRCUIT_MISMATCH')
 require(digest(read_bounded(directory/'verification-key.bin',16*1024*1024))==job['verificationKeyDigest'],'VERIFICATION_KEY_MISMATCH')
 manifest=load(read_bounded(directory/'manifest.json'))
 for name in ('proof.bin','verification-key.bin','circuit.r1cs','public-witness.bin'):
  require(digest(read_bounded(directory/name,16*1024*1024))==manifest.get(name+'Sha256'),'RESULT_DIGEST_MISMATCH')
 invoke([ROOT/'prover-worker/build/verify',directory,job['publicInputs']['y']])
 return {'schema':'deproof-verification-observation-v1','status':'VALID','verifier':'gnark-v0.14.0 independent CLI','verificationKeyDigest':job['verificationKeyDigest'],'publicInputs':job['publicInputs'],'observedAt':timestamp(),'trustDomain':'SAME_OWNER_HOST; SEPARATE_VERIFIER_PROCESS; EDUCATIONAL_TRUSTED_SETUP'}
def run(job_path,owner_path,job_id,output):
 job,setup=validate(job_path,owner_path);require(job['id']==job_id,'JOB_ID_MISMATCH')
 output=pathlib.Path(output);output.mkdir(mode=0o700,parents=True,exist_ok=False)
 events=[]
 def event(state):
  events.append({'state':state,'at':timestamp()});atomic_json(output/'events.json',events)
 for state in ['DISCOVERED','VALIDATED','ACCEPTED','RUNNING']:event(state)
 try:
  result=output/'result';invoke([ROOT/'prover-worker/build/prove',result,setup]);event('RESULT_READY')
  verification=verify_result(result,job);event('VERIFIED')
  receipt={'schema':'deproof-contribution-v1','id':str(uuid.uuid4()),'producerFingerprint':None,'jobId':job['id'],'inputCommitment':job['inputCommitment'],'resultDigest':digest((result/'proof.bin').read_bytes()),'meteringDigest':None,'verificationObservation':verification,'chainObservation':None,'assurance':'LOCAL_CRYPTOGRAPHIC_RELATION_ONLY; NO_PROVIDER_ACCEPTANCE_OR_REWARD; HOST_ISOLATION_NOT_QUALIFIED','createdAt':timestamp(),'job':job,'jobEnvelope':load(read_bounded(job_path)),'resultManifest':load((result/'manifest.json').read_bytes()),'artifactDirectory':str(result)}
  atomic_json(output/'receipt.json',receipt);event('DELIVERED');return receipt
 except Exception:
  event('EXECUTION_FAILED');raise

def main():
 p=argparse.ArgumentParser();sub=p.add_subparsers(dest='mode',required=True)
 c=sub.add_parser('create');c.add_argument('directory')
 for mode in ['discover','run']:
  c=sub.add_parser(mode);c.add_argument('--job',required=True);c.add_argument('--owner',required=True)
  if mode=='run':c.add_argument('--job-id',required=True);c.add_argument('--output',required=True)
 a=p.parse_args()
 if a.mode=='create':result=create(a.directory)
 elif a.mode=='discover':result=validate(a.job,a.owner)[0]
 else:result=run(a.job,a.owner,a.job_id,a.output)
 print(json.dumps(result))
if __name__=='__main__':main()
