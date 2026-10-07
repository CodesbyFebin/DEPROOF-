#!/usr/bin/env python3
"""Local owner-approved node client. Private node-control key is not a wallet key.
TLS trust requires the certificate copied from the owner-controlled node state.
"""
import argparse,base64,json,pathlib,ssl,urllib.request,uuid,datetime,os,sys
from cryptography.hazmat.primitives.asymmetric import ed25519
from cryptography.hazmat.primitives import serialization

def main():
 p=argparse.ArgumentParser();p.add_argument('--certificate',required=True);p.add_argument('--state',default='.deproof-client');sub=p.add_subparsers(dest='action',required=True)
 pair=sub.add_parser('pair');pair.add_argument('--challenge',required=True,help='Challenge JSON file obtained from local owner console');pair.add_argument('--code',required=True);pair.add_argument('--fingerprint',required=True);pair.add_argument('--scopes',default='READ_NODE')
 cmd=sub.add_parser('command');cmd.add_argument('operation',choices=['observe','consent','transfer','stop','export']);cmd.add_argument('--params',default='{}')
 a=p.parse_args();root=pathlib.Path(a.state);root.mkdir(mode=0o700,parents=True,exist_ok=True)
 if root.stat().st_mode & 0o077:raise ValueError('INSECURE_CLIENT_STATE')
 keyfile=root/'client.key'
 if keyfile.exists():
  if keyfile.stat().st_mode & 0o077:raise ValueError('INSECURE_CLIENT_KEY')
  key=ed25519.Ed25519PrivateKey.from_private_bytes(keyfile.read_bytes())
 else:
  key=ed25519.Ed25519PrivateKey.generate();fd=os.open(keyfile,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600)
  with os.fdopen(fd,'wb') as out:out.write(key.private_bytes(serialization.Encoding.Raw,serialization.PrivateFormat.Raw,serialization.NoEncryption()))
 enc=lambda b:base64.b64encode(b).decode();pub=enc(key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw))
 context=ssl.create_default_context(cafile=a.certificate);context.minimum_version=ssl.TLSVersion.TLSv1_3
 def send(path,data):
  req=urllib.request.Request('https://127.0.0.1:9843'+path,json.dumps(data,separators=(',',':')).encode(),{'Content-Type':'application/json'},method='POST')
  with urllib.request.urlopen(req,context=context,timeout=90) as response:return json.loads(response.read(65536))
 if a.action=='pair':
  c=json.loads(pathlib.Path(a.challenge).read_text());assert c['fingerprint']==a.fingerprint,'HOST_FINGERPRINT_MISMATCH';scopes=a.scopes.split(',')
  # Matches the domain-separated struct order of PairPayload, not receipt JCS.
  payload=json.dumps({'domain':'deproof-pair-v1','challenge':{k:c[k] for k in ('id','nonce','fingerprint','expiresAt')},'publicKey':pub,'scopes':scopes},separators=(',',':')).encode()
  result=send('/pair',{'challengeId':c['id'],'code':a.code,'publicKey':pub,'signatureBase64':enc(key.sign(payload)),'approvedFingerprint':a.fingerprint,'scopes':scopes})
  fd=os.open(root/'session.json',os.O_WRONLY|os.O_CREAT|os.O_TRUNC,0o600)
  with os.fdopen(fd,'w') as out:json.dump(result,out)
 else:
  session=json.loads((root/'session.json').read_text());deadline=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(seconds=45)).isoformat().replace('+00:00','Z')
  payload=json.dumps({'sessionId':session['sessionId'],'operationId':str(uuid.uuid4()),'deadline':deadline,'policy':'node-policy-v1','action':a.operation,'params':json.loads(a.params)},separators=(',',':')).encode()
  result=send('/command',{'payloadBase64':enc(payload),'signatureBase64':enc(key.sign(payload))})
 print(json.dumps(result,indent=2))
if __name__=='__main__':
 try:main()
 except Exception as e:print('FAIL: '+str(e),file=sys.stderr);sys.exit(1)
