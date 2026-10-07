#!/usr/bin/env python3
"""Portable public receipt bundles and explicit encrypted backups.
Private evidence inclusion needs --include-private-files. Wallet/evidence keys are never packaged.
"""
import argparse,base64,json,os,pathlib,secrets,sys
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from verify import load,verify_receipt,digest,require
DOMAIN=b'deproof-encrypted-bundle-v1'
LIMIT=128*1024*1024

def create_bundle(receipt_paths,private_files=None):
 records=[];files={}
 for path in receipt_paths:
  raw=pathlib.Path(path).read_bytes();record=load(raw)
  require(record.get('schema')=='deproof-receipt-v2','UNSUPPORTED_RECEIPT');verify_receipt(record,private_files)
  records.append({'sha256':digest(raw),'rawJsonBase64':base64.b64encode(raw).decode()})
  if private_files and record.get('manifest'):
   root=pathlib.Path(private_files).resolve()
   for f in record['manifest']['files']:
    p=(root/f['id']).resolve();require(p.parent==root and p.is_file(),'PRIVATE_FILE_UNAVAILABLE')
    require(p.stat().st_size<=64*1024*1024,"PRIVATE_FILE_TOO_LARGE");rawfile=p.open("rb").read(64*1024*1024+1);require(len(rawfile)<=64*1024*1024,'PRIVATE_FILE_TOO_LARGE');require(digest(rawfile)==f['sha256'],'FILE_DIGEST_MISMATCH')
    existing=files.get(f['id']);encoded=base64.b64encode(rawfile).decode();require(existing is None or existing==encoded,'FILE_ID_COLLISION');files[f['id']]=encoded
 package={'schema':'deproof-verification-bundle-v1','records':records,'privateFiles':files,'assurance':'Signature/file integrity only. No remote hardware attestation, physical truth or chain proof.'}
 raw=json.dumps(package,ensure_ascii=False,separators=(',',':')).encode();require(len(raw)<=LIMIT,'BUNDLE_TOO_LARGE');return raw

def verify_bundle(raw):
 require(len(raw)<=LIMIT,'BUNDLE_TOO_LARGE')
 # Bundles may contain many records; enforce the bundle limit before decoding separately.
 package=json.loads(raw,object_pairs_hook=__import__('verify').duplicate_safe);require(package['schema']=='deproof-verification-bundle-v1','BUNDLE_SCHEMA')
 files=package['privateFiles'];require(isinstance(files,dict),'BUNDLE_FILES')
 reports=[]
 for record in package['records']:
  original=base64.b64decode(record['rawJsonBase64'],validate=True);require(digest(original)==record['sha256'],'RAW_RECEIPT_CHANGED');r=load(original);result=verify_receipt(r)
  if r.get('manifest'):
   for f in r['manifest']['files']:
    if f['id'] in files:
     data=base64.b64decode(files[f['id']],validate=True);require(digest(data)==f['sha256'] and str(len(data))==f['byteLength'],'FILE_DIGEST_MISMATCH')
     for o in result['manifest']['files']:
      if o['id']==f['id']:o['status']='PASS';o.pop('reason',None)
  reports.append(result)
 return reports

def encrypt_bundle(raw,key):
 require(len(key)==32,'BACKUP_KEY_SIZE');verify_bundle(raw);nonce=secrets.token_bytes(12)
 ciphertext=AESGCM(key).encrypt(nonce,raw,DOMAIN)
 return json.dumps({'schema':DOMAIN.decode(),'algorithm':'AES-256-GCM','nonceBase64':base64.b64encode(nonce).decode(),'ciphertextBase64':base64.b64encode(ciphertext).decode()},separators=(',',':')).encode()

def decrypt_bundle(raw,key):
 require(len(raw)<=2*LIMIT,'BACKUP_TOO_LARGE');header=json.loads(raw,object_pairs_hook=__import__('verify').duplicate_safe);require(set(header)=={'schema','algorithm','nonceBase64','ciphertextBase64'} and header['schema']==DOMAIN.decode() and header['algorithm']=='AES-256-GCM','BACKUP_SCHEMA')
 nonce=base64.b64decode(header['nonceBase64'],validate=True);require(len(nonce)==12,'BACKUP_NONCE_SIZE')
 plaintext=AESGCM(key).decrypt(nonce,base64.b64decode(header['ciphertextBase64'],validate=True),DOMAIN);verify_bundle(plaintext);return plaintext

def exclusive(path,data):
 fd=os.open(path,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'wb') as out:out.write(data);out.flush();os.fsync(out.fileno())

def main():
 p=argparse.ArgumentParser();s=p.add_subparsers(dest='action',required=True)
 k=s.add_parser('create-key');k.add_argument('output')
 b=s.add_parser('export');b.add_argument('output');b.add_argument('receipts',nargs='+');b.add_argument('--include-private-files',metavar='DIRECTORY')
 v=s.add_parser('verify');v.add_argument('input')
 for action in ['encrypt','decrypt']:
  c=s.add_parser(action);c.add_argument('input');c.add_argument('output');c.add_argument('--key',required=True)
 a=p.parse_args()
 if a.action=='create-key':exclusive(a.output,secrets.token_bytes(32));print('Created owner-controlled 32-byte backup key. Store an offline copy separately; loss prevents decryption. This is not a wallet or evidence private key.');return
 if a.action=='export':raw=create_bundle(a.receipts,a.include_private_files);exclusive(a.output,raw);print('Created bundle; private files included only if explicitly requested.');return
 raw=pathlib.Path(a.input).read_bytes()
 if a.action=='verify':print(json.dumps(verify_bundle(raw),indent=2));return
 keypath=pathlib.Path(a.key);require(keypath.stat().st_mode & 0o077==0,'INSECURE_BACKUP_KEY_FILE');key=keypath.read_bytes();result=encrypt_bundle(raw,key) if a.action=='encrypt' else decrypt_bundle(raw,key);exclusive(a.output,result);print('PASS: authenticated '+a.action+'; exact original receipt bytes preserved.')
if __name__=='__main__':
 try:main()
 except Exception as e:print('FAIL: '+(str(e) or type(e).__name__),file=sys.stderr);sys.exit(1)
