import base64,datetime,json,pathlib,tempfile,unittest
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives.serialization import Encoding,PublicFormat
from proof_jobs import validate
from verify import canonical,digest
class SignedJobTest(unittest.TestCase):
 def fixture(self,root,**changes):
  key=Ed25519PrivateKey.generate();public=key.public_key().public_bytes(Encoding.Raw,PublicFormat.Raw)
  setup=root/'setup';setup.mkdir();(setup/'circuit.r1cs').write_bytes(b'clearly synthetic schema fixture');(setup/'verification-key.bin').write_bytes(b'not a real proof key')
  job={'schema':'deproof-proof-job-v1','id':'test','backend':'gnark-v0.14.0','scheme':'Groth16/BN254','circuitDigest':digest((setup/'circuit.r1cs').read_bytes()),'verificationKeyDigest':digest((setup/'verification-key.bin').read_bytes()),'inputCommitment':digest(canonical({'x':'3','y':'35'})),'publicInputs':{'y':'35'},'maxWitnessBytes':'32','deadline':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(minutes=1)).isoformat().replace('+00:00','Z'),'source':'SYNTHETIC_UNIT_FIXTURE','compensation':None}
  job.update(changes)
  if job.get('resourceProfile')=='CURRENT_DIGEST':job['resourceProfile']={'circuit':'owner-local-cubic','circuitVersion':job['circuitDigest'],'minMemoryMiB':'256','minCpuCores':'1'}
  raw=canonical(job);envelope={'schema':'deproof-signed-job-v1','payloadBase64':base64.b64encode(raw).decode(),'signatureBase64':base64.b64encode(key.sign(raw)).decode(),'publicKeyBase64':base64.b64encode(public).decode()}
  path=root/'job.json';path.write_text(json.dumps(envelope));owner=root/'owner.key';owner.write_text(base64.b64encode(public).decode());return path,owner
 def test_authentic_bounded_profile_is_accepted_for_discovery_only(self):
  with tempfile.TemporaryDirectory() as t:
   p,o=self.fixture(pathlib.Path(t));self.assertEqual(validate(p,o)[0]['id'],'test')
 def test_expired_backend_mismatch_witness_and_public_input_are_denied(self):
  for changes in ({'deadline':'2000-01-01T00:00:00Z'},{'backend':'arbitrary-executable'},{'maxWitnessBytes':'9999999999'},{'publicInputs':{'y':'36'}}):
   with tempfile.TemporaryDirectory() as t:
    p,o=self.fixture(pathlib.Path(t),**changes)
    with self.assertRaises(ValueError):validate(p,o)
 def test_pinned_setup_cannot_change_after_signed_discovery(self):
  with tempfile.TemporaryDirectory() as t:
   root=pathlib.Path(t);p,o=self.fixture(root);(root/'setup/verification-key.bin').write_bytes(b'changed')
   with self.assertRaises(ValueError):validate(p,o)

 def test_signed_resource_profile_is_explicitly_supported(self):
  with tempfile.TemporaryDirectory() as t:
   p,o=self.fixture(pathlib.Path(t),resourceProfile='CURRENT_DIGEST');job,_=validate(p,o);self.assertEqual(job['resourceProfile']['circuitVersion'],job['circuitDigest']);self.assertEqual(job['resourceProfile']['minMemoryMiB'],'256')
 def test_signed_malformed_resource_profile_is_not_silently_ignored(self):
  for profile in ({'circuit':'external'}, {'circuit':'owner-local-cubic','circuitVersion':'0'*64,'minMemoryMiB':'256','minCpuCores':'1'}, {'circuit':'owner-local-cubic','circuitVersion':'0'*64,'minMemoryMiB':'0','minCpuCores':'1'}):
   with tempfile.TemporaryDirectory() as t:
    p,o=self.fixture(pathlib.Path(t),resourceProfile=profile)
    with self.assertRaisesRegex(ValueError,'RESOURCE_PROFILE_DENIED'):validate(p,o)
