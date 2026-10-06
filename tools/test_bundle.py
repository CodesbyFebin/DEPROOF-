import unittest,pathlib,tempfile,secrets,sys,json,base64
sys.path.insert(0,str(pathlib.Path(__file__).parent))
from bundle import *
import test_verifier as fixtures
from cryptography.exceptions import InvalidTag
class BundleTests(unittest.TestCase):
 def test_exact_receipt_bytes_roundtrip_and_wrong_key_tamper(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)/'receipt.json';r=fixtures.VerifierTests().record();raw=json.dumps(r,ensure_ascii=False,indent=3).encode();p.write_bytes(raw)
   package=create_bundle([p]);key=secrets.token_bytes(32);encrypted=encrypt_bundle(package,key);decrypted=decrypt_bundle(encrypted,key)
   self.assertEqual(decrypted,package);self.assertEqual(base64.b64decode(json.loads(decrypted)['records'][0]['rawJsonBase64']),raw)
   with self.assertRaises(InvalidTag):decrypt_bundle(encrypted,secrets.token_bytes(32))
   e=json.loads(encrypted);cipher=bytearray(base64.b64decode(e['ciphertextBase64']));cipher[-1]^=1;e['ciphertextBase64']=base64.b64encode(cipher).decode()
   with self.assertRaises(InvalidTag):decrypt_bundle(json.dumps(e).encode(),key)
 def test_private_files_off_by_default_and_paths_denied(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)/'receipt.json';p.write_text(json.dumps(fixtures.VerifierTests().record()))
   package=json.loads(create_bundle([p]));self.assertEqual(package['privateFiles'],{})
 def test_changed_raw_receipt_digest_denied(self):
  with tempfile.TemporaryDirectory() as d:
   p=pathlib.Path(d)/'receipt.json';p.write_text(json.dumps(fixtures.VerifierTests().record()));b=json.loads(create_bundle([p]));b['records'][0]['sha256']='0'*64
   with self.assertRaises(ValueError):verify_bundle(json.dumps(b).encode())
if __name__=='__main__':unittest.main()
