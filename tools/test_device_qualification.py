import importlib.util,pathlib,types,unittest
s=importlib.util.spec_from_file_location('device',pathlib.Path(__file__).resolve().parents[1]/'scripts/qualify-device.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
class DeviceQualificationTests(unittest.TestCase):
 def args(self,**kw):return types.SimpleNamespace(adb='/fixture/adb',serial=None,install=False,**kw)
 def test_no_device_keeps_every_gate_unrun(self):
  result=m.run(self.args(),lambda *a,**k:types.SimpleNamespace(returncode=0,stdout='List of devices attached\n'))
  self.assertEqual('BLOCKED',result['preflight']);self.assertTrue(all(c['status']=='NOT_RUN' for c in result['checks'].values()))
 def test_unauthorized_and_ambiguous_devices_do_not_install(self):
  for text in ['List of devices attached\na unauthorized\n','a device\nb device\n']:
   calls=[]
   def adb(cmd,**kw):calls.append(cmd);return types.SimpleNamespace(returncode=0,stdout=text)
   result=m.run(self.args(),adb);self.assertEqual('BLOCKED',result['preflight']);self.assertEqual(1,len(calls))
 def test_emulator_cannot_qualify_hardware(self):
  def adb(cmd,**kw):return types.SimpleNamespace(returncode=0,stdout='a device\n' if cmd[1]=='devices' else '1\n')
  result=m.run(self.args(),adb);self.assertEqual('BLOCKED',result['preflight']);self.assertIn('Emulator',result['reason'])
 def test_inventory_ignores_daemon_noise(self):
  self.assertEqual([('a','device'),('b','offline')],m.inventory('* daemon started successfully\nList of devices attached\na device usb:1\nb offline\n'))
