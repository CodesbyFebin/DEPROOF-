import unittest,copy,hashlib,json,pathlib
from verify import verify_manifest,canonical
class LocationTests(unittest.TestCase):
 def manifest(self):
  return {'schema':'deproof-evidence-v2','taskId':None,'messageSha256':None,'files':[],'note':'exact\nनोट','createdAt':'2026-10-06T10:00:00Z','location':{'latitude':'10.5','longitude':'76.25','accuracyMeters':'80','provider':'network','observedAt':'2026-10-06T09:59:58Z','collectedAt':'2026-10-06T10:00:00Z','permission':'APPROXIMATE','mock':False}}
 def test_optional_location_old_manifests_and_mock_are_honest(self):
  m=self.manifest();result=verify_manifest(m);self.assertEqual('EVIDENCE_ONLY',result['binding']);m.pop('location');verify_manifest(m)
  m=self.manifest();m['location']['mock']=True;verify_manifest(m)
 def test_out_of_range_nan_numbers_stale_and_unknown_metadata_refused(self):
  for field,value in [('latitude','91'),('longitude','NaN'),('accuracyMeters',80),('provider','invented'),('mock','false'),('observedAt','2026-10-06T09:00:00Z')]:
   m=self.manifest();m['location'][field]=value
   with self.assertRaises((ValueError,TypeError)):verify_manifest(m)
  m=self.manifest();m['location']['truth']='verified'
  with self.assertRaises(ValueError):verify_manifest(m)
 def test_cross_language_location_golden_digest(self):
  vector=json.loads((pathlib.Path(__file__).resolve().parents[1]/'fixtures/location-golden.json').read_text())
  self.assertEqual(vector['canonicalUtf8'],canonical(vector['manifest']).decode());self.assertEqual(vector['sha256'],verify_manifest(vector['manifest'])['manifestSha256'])
