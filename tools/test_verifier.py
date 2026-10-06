import unittest, pathlib, json, hashlib, base64, sys, copy
sys.path.insert(0,str(pathlib.Path(__file__).parent))
from verify import *
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec
class VerifierTests(unittest.TestCase):
 def manifest(self):return {'schema':'deproof-evidence-v2','taskId':None,'messageSha256':None,'files':[],'note':'नमस्ते\n😀','createdAt':'2026-10-06T00:00:00Z'}
 def record(self):
  m=self.manifest();d=digest(canonical(m));env=canonical({'domain':'deproof-evidence-sig-v2','manifestDigest':d,'manifestSchema':'deproof-evidence-v2','algorithm':'SHA256withECDSA'});key=ec.generate_private_key(ec.SECP256R1());sig=key.sign(env,ec.ECDSA(hashes.SHA256()));enc=lambda b:base64.b64encode(b).decode()
  return {'schema':'deproof-receipt-v2','id':str(uuid.uuid4()),'outcome':'LOCAL_EVIDENCE_SIGNED','taskId':None,'createdAt':'2026-10-06T00:00:00Z','cardHash':None,'messageSha256':None,'currentMessageSha256':None,'reviewContextHash':None,'evidenceDigest':d,'manifest':m,'signature':None,'localSignature':{'domain':'deproof-evidence-sig-v2','algorithm':'SHA256withECDSA','envelopeBase64':enc(env),'signatureDerBase64':enc(sig),'spkiBase64':enc(key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)),'securityLevel':'SOFTWARE_TEST_ONLY'},'submission':{'state':'NOT_SUBMITTED','broadcast':False,'submittedByDeproof':False,'attemptedAt':None,'rpcAcceptedAt':None},'chainObservation':{'availability':'NOT_QUERIED','lastKnownStatus':'UNKNOWN'}}
 def test_real_test_only_signature_and_tampered_note(self):
  r=self.record();self.assertEqual(verify_receipt(r)['localSignature'],'PASS');r['manifest']['note']+='changed'
  with self.assertRaises(ValueError):verify_receipt(r)
 def test_wrong_domain_key_and_signature(self):
  r=self.record();r['localSignature']['domain']='legacy-digest-bytes-v1'
  with self.assertRaises(ValueError):verify_receipt(r)
  r=self.record();r['localSignature']['signatureDerBase64']=base64.b64encode(bytes(64)).decode()
  with self.assertRaises((ValueError,InvalidSignature)):verify_receipt(r)
 def test_duplicate_keys_and_numbers(self):
  with self.assertRaises(ValueError):load('{"a":1,"a":2}')
  with self.assertRaises(ValueError):canonical({'n':9007199254740993})
 def test_golden_jcs(self):
  p=pathlib.Path(__file__).parents[1]/'fixtures/jcs-vectors.json'
  for v in load(p.read_bytes()):self.assertEqual(canonical(v['input']).decode(),v['canonical']);self.assertEqual(digest(canonical(v['input'])),v['sha256'])
 def test_node_signature_tamper_and_fingerprint(self):
  key=ed25519.Ed25519PrivateKey.generate();pub=key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw);payload=canonical({'domain':'deproof-metering-v1','nodeFingerprint':digest(pub),'sequence':'1','peerAcknowledgment':None})
  r={'schema':'deproof-node-signed-v1','payloadBase64':base64.b64encode(payload).decode(),'signatureBase64':base64.b64encode(key.sign(payload)).decode(),'publicKeyBase64':base64.b64encode(pub).decode()}
  self.assertEqual(verify_node(r)['integrity'],'PASS');r['payloadBase64']=base64.b64encode(payload+b' ').decode()
  with self.assertRaises(InvalidSignature):verify_node(r)
 def test_raw_file_changed_and_missing(self):
  import tempfile
  with tempfile.TemporaryDirectory() as directory:
   data=b'raw file bytes';p=pathlib.Path(directory)/'file1';p.write_bytes(data);m=self.manifest();m['files']=[{'id':'file1','sha256':digest(data),'mime':'application/octet-stream','byteLength':str(len(data)),'provenance':'import'}]
   self.assertEqual(verify_manifest(m,directory)['files'][0]['status'],'PASS');p.write_bytes(data+b'changed')
   with self.assertRaises(ValueError):verify_manifest(m,directory)
   p.unlink();self.assertEqual(verify_manifest(m,directory)['files'][0]['status'],'NOT_RUN')
if __name__=='__main__':unittest.main()
