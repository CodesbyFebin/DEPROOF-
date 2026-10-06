#!/usr/bin/env python3
"""Independent offline integrity verifier. No RPC, account, upload or wallet keys.
Restricted JCS profile deliberately forbids numeric JSON values in signed manifests.
"""
import argparse, base64, hashlib, json, pathlib, sys, uuid, datetime, re
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.hazmat.primitives.asymmetric import ec, ed25519
from cryptography.hazmat.primitives import hashes
from cryptography.exceptions import InvalidSignature

def duplicate_safe(pairs):
    out={}
    for k,v in pairs:
        if k in out: raise ValueError('DUPLICATE_JSON_KEY')
        out[k]=v
    return out

def load(raw):
    if len(raw)>2*1024*1024: raise ValueError('JSON_TOO_LARGE')
    return json.loads(raw,object_pairs_hook=duplicate_safe,parse_constant=lambda x:(_ for _ in ()).throw(ValueError('NON_FINITE_JSON')))

def canonical(value):
    def transform(v):
        if isinstance(v,dict):return {k:transform(v[k]) for k in sorted(v,key=lambda x:x.encode('utf-16-be'))}
        if isinstance(v,list):return [transform(x) for x in v]
        if v is None or isinstance(v,(str,bool)):return v
        raise ValueError('JCS_PROFILE_NUMBERS_FORBIDDEN')
    # JSON library escapes strings; UTF-8 encoding refuses lone surrogates.
    return json.dumps(transform(value),ensure_ascii=False,separators=(',',':')).encode('utf-8')

def digest(b):return hashlib.sha256(b).hexdigest()
def b64(s):return base64.b64decode(s,validate=True)
def hex64(s):return isinstance(s,str) and len(s)==64 and all(c in '0123456789abcdef' for c in s)
def require(ok,code):
    if not ok:raise ValueError(code)

def verify_manifest(m,files=None):
    fields={'schema','taskId','messageSha256','files','note','createdAt'}
    require(set(m) in (fields,fields|{'location'}),'MANIFEST_FIELDS')
    if 'location' in m:
        from decimal import Decimal
        l=m['location'];require(isinstance(l,dict) and set(l)=={'latitude','longitude','accuracyMeters','provider','observedAt','collectedAt','permission','mock'},'LOCATION_FIELDS')
        for field,lo,hi in [('latitude',-90,90),('longitude',-180,180),('accuracyMeters',0,100000)]:
            require(isinstance(l[field],str) and re.fullmatch(r'-?(0|[1-9][0-9]*)(\.[0-9]{1,12})?',l[field]) is not None,'LOCATION_DECIMAL')
            require(Decimal(lo)<=Decimal(l[field])<=Decimal(hi),'LOCATION_RANGE')
        require(l['provider'] in ('gps','network','fused') and l['permission'] in ('APPROXIMATE','PRECISE') and type(l['mock']) is bool,'LOCATION_PROVENANCE')
        observed=datetime.datetime.fromisoformat(l['observedAt'].replace('Z','+00:00'));collected=datetime.datetime.fromisoformat(l['collectedAt'].replace('Z','+00:00'))
        require(observed.tzinfo is not None and collected.tzinfo is not None and -5 <= (collected-observed).total_seconds() <=120,'LOCATION_STALE')
    require(m['schema']=='deproof-evidence-v2' and isinstance(m['note'],str),'MANIFEST_SCHEMA')
    require(m['messageSha256'] is None or hex64(m['messageSha256']),'MESSAGE_DIGEST')
    require(m['taskId'] is None or isinstance(m['taskId'],str),'TASK_ID')
    require(isinstance(m['createdAt'],str) and m['createdAt'].endswith('Z'),'TIMESTAMP_UTC')
    datetime.datetime.fromisoformat(m['createdAt'].replace('Z','+00:00'))
    ids=[f['id'] for f in m['files']]
    require(ids==sorted(ids) and len(ids)==len(set(ids)),'FILE_ORDER_OR_DUPLICATE')
    observations=[]
    for f in m['files']:
        require(set(f)=={'id','sha256','mime','byteLength','provenance'},'FILE_FIELDS')
        require(hex64(f['sha256']) and f['byteLength'].isdigit() and str(int(f['byteLength']))==f['byteLength'] and f['provenance'] in ('import','capture'),'FILE_METADATA')
        require(f['id'] and len(f['id'])<=80 and all(c.isascii() and (c.isalnum() or c in '_-') for c in f['id']),'FILE_ID')
        if files:
            root=pathlib.Path(files).resolve();p=(root/f['id']).resolve();require(p.parent==root,'PATH_TRAVERSAL')
            if p.is_file():
                h=hashlib.sha256();count=0
                with p.open('rb') as source:
                    while chunk:=source.read(65536):h.update(chunk);count+=len(chunk)
                require(h.hexdigest()==f['sha256'] and str(count)==f['byteLength'],'FILE_DIGEST_MISMATCH')
                observations.append({'id':f['id'],'status':'PASS'})
            else:observations.append({'id':f['id'],'status':'NOT_RUN','reason':'PRIVATE_FILE_NOT_PROVIDED'})
        else:observations.append({'id':f['id'],'status':'NOT_RUN','reason':'PRIVATE_FILE_NOT_PROVIDED'})
    return {'manifestSha256':digest(canonical(m)),'files':observations,'binding':'EVIDENCE_ONLY' if m['messageSha256'] is None else 'MESSAGE_DIGEST_LINK; message bytes need separate verification'}

def verify_receipt(r,files=None):
    require(r.get('schema')=='deproof-receipt-v2','RECEIPT_SCHEMA');uuid.UUID(r['id'])
    require(r['createdAt'].endswith('Z'),'TIMESTAMP_UTC');datetime.datetime.fromisoformat(r['createdAt'].replace('Z','+00:00'))
    require(r['outcome'] in ('REJECTED','OBSERVED','LOCAL_EVIDENCE_SIGNED','WALLET_SIGNED','SUBMITTED'),'OUTCOME')
    for field in ('cardHash','messageSha256','currentMessageSha256','reviewContextHash','evidenceDigest'):require(field in r and (r[field] is None or hex64(r[field])),'DIGEST_'+field)
    s=r['submission'];require(s['state'] in ('NOT_SUBMITTED','SUBMITTING','SUBMITTED','SUBMISSION_UNKNOWN'),'SUBMISSION_STATE')
    o=r['chainObservation'];require(o['availability'] in ('AVAILABLE','UNAVAILABLE','NOT_QUERIED') and o['lastKnownStatus'] in ('UNKNOWN','PROCESSED','CONFIRMED','FINALIZED','FAILED'),'CHAIN_OBSERVATION')
    if r['outcome']=='REJECTED':require(r['signature'] is None and s['state']=='NOT_SUBMITTED' and s['broadcast'] is False and s['submittedByDeproof'] is False,'REJECTION_PROVENANCE')
    if r['outcome']=='OBSERVED':require(s['state']=='NOT_SUBMITTED' and s['submittedByDeproof'] is False and s['broadcast'] is False,'OBSERVATION_PROVENANCE')
    if r['outcome']=='SUBMITTED':require(r['signature'] is not None and s['state']=='SUBMITTED' and s['broadcast'] is True and s['submittedByDeproof'] is True and s['rpcAcceptedAt'] is not None,'SUBMISSION_PROVENANCE')
    if r['outcome']=='LOCAL_EVIDENCE_SIGNED':require(r['localSignature'] is not None and r['manifest'] is not None,'MISSING_LOCAL_SIGNATURE')
    result={'integrity':'PASS','chainObservation':'NOT_VERIFIED_OFFLINE','physicalTruth':'NOT_ESTABLISHED'}
    if r.get('manifest') is not None:
        result['manifest']=verify_manifest(r['manifest'],files);require(result['manifest']['manifestSha256']==r['evidenceDigest'],'MANIFEST_DIGEST_MISMATCH')
    if r.get('localSignature'):
        s=r['localSignature'];require(s['domain']=='deproof-evidence-sig-v2' and s['algorithm']=='SHA256withECDSA','SIGNATURE_DOMAIN_OR_ALGORITHM')
        expected=canonical({'domain':'deproof-evidence-sig-v2','manifestDigest':r['evidenceDigest'],'manifestSchema':'deproof-evidence-v2','algorithm':'SHA256withECDSA'})
        payload=b64(s['envelopeBase64']);require(payload==expected,'ENVELOPE_MISMATCH')
        key=load_der_public_key(b64(s['spkiBase64']));require(isinstance(key,ec.EllipticCurvePublicKey) and isinstance(key.curve,ec.SECP256R1),'WRONG_KEY_CURVE')
        key.verify(b64(s['signatureDerBase64']),payload,ec.ECDSA(hashes.SHA256()))
        result['localSignature']='PASS';result['hardwareClaim']='LOCAL_METADATA_ONLY; NOT_REMOTE_ATTESTATION'
    return result

def verify_node(r):
    require(r['schema']=='deproof-node-signed-v1','NODE_SCHEMA');payload=b64(r['payloadBase64']);pub=b64(r['publicKeyBase64'])
    ed25519.Ed25519PublicKey.from_public_bytes(pub).verify(b64(r['signatureBase64']),payload)
    event=load(payload);require(event['domain'] in ('deproof-metering-v1','deproof-contribution-v1'),'NODE_DOMAIN')
    require(event['nodeFingerprint']==digest(pub),'NODE_FINGERPRINT')
    if event['domain']=='deproof-contribution-v1':
        require(event['producerFingerprint']==digest(pub) and hex64(event['inputCommitment']) and hex64(event['resultDigest']),'CONTRIBUTION_FIELDS')
        return {'integrity':'PASS','domain':event['domain'],'proofVerification':'NOT_RUN_ARTIFACTS_NOT_PROVIDED','verificationObservation':'PRODUCER_REPORTED; INDEPENDENT_ARTIFACT_VERIFICATION_REQUIRED','reward':'NOT_ESTABLISHED'}
    require(event['sequence'].isdigit(),'SEQUENCE')
    return {'integrity':'PASS','domain':event['domain'],'measurementTruth':'NOT_ESTABLISHED','peerCorroboration':'NOT_RUN' if event.get('peerAcknowledgment') is None else 'NOT_VERIFIED','reward':'NOT_ESTABLISHED'}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('record');parser.add_argument('--files');args=parser.parse_args()
    r=load(pathlib.Path(args.record).read_bytes())
    if r.get('schema')=='deproof-portable-event-v1':
        result=verify_receipt(r['event'],args.files);result['observations']='UNSIGNED_APPEND_ONLY_LOCAL_OBSERVATIONS; NOT_CHAIN_PROOF'
    elif r.get('schema')=='deproof-node-signed-v1':result=verify_node(r)
    else:result=verify_receipt(r,args.files)
    print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':
    try:main()
    except (ValueError,KeyError,TypeError,InvalidSignature,OSError) as e:print('FAIL: '+(str(e) or 'INVALID_SIGNATURE'),file=sys.stderr);sys.exit(1)
