#!/usr/bin/env python3
"""Non-destructive physical-device preflight; manual security gates are never inferred."""
import argparse,datetime,hashlib,json,pathlib,shutil,subprocess,sys
ROOT=pathlib.Path(__file__).resolve().parents[1]
GATES=['APK_INSTALL_LAUNCH','WALLET_HANDOFF','MESSAGE_MUTATION_REFUSAL','RETURNED_SIGNED_BYTES','KEYSTORE_SECURITY_LEVEL','PERMISSION_DENIAL','PROCESS_DEATH','ROOM_MIGRATION','CRASH_RECOVERY','LOCAL_WORKFLOWS']
def inventory(text):
 return [(p[0],p[1]) for line in text.splitlines() if len(p:=line.split())>=2 and p[1] in ['device','offline','unauthorized']]
def report_base():
 return {'schema':'deproof-device-qualification-v1','generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'credit':'Built by CodesbyFebin','status':'NOT_RUN','checks':{g:{'status':'NOT_RUN','reason':'Requires observed physical-device execution and checklist evidence.'} for g in GATES},'automatedActions':[],'limitations':['ADB command success is not wallet, hardware, migration or crash qualification.','No mainnet transactions; use a dedicated devnet wallet and disposable fixture records.']}
def run(args,adb_run=None):
 result=report_base();adb=args.adb or shutil.which('adb')
 if not adb:
  candidate=pathlib.Path.home()/'Library/Android/sdk/platform-tools/adb'
  if candidate.is_file():adb=str(candidate)
 def command(*cmd):
  try:
   p=(adb_run or subprocess.run)([adb,*cmd],capture_output=True,text=True,timeout=45)
   if p.returncode:raise RuntimeError('ADB command failed: '+cmd[0])
   return p.stdout
  except (OSError,subprocess.TimeoutExpired) as e:raise RuntimeError('ADB unavailable or timed out') from e
 try:
  if not adb:raise RuntimeError('Android platform-tools unavailable')
  devices=inventory(command('devices','-l'));ready=[serial for serial,state in devices if state=='device']
  result['deviceInventory']=[{'serial':serial,'state':state} for serial,state in devices]
  if not ready:raise RuntimeError('No authorized Android device connected; '+str(len(devices))+' offline/unauthorized devices listed')
  if args.serial:
   if args.serial not in ready:raise RuntimeError('Selected device is not authorized/connected')
   serial=args.serial
  else:
   if len(ready)!=1:raise RuntimeError('Expected one authorized physical device; select --serial when multiple are attached')
   serial=ready[0]
  if command('-s',serial,'shell','getprop','ro.kernel.qemu').strip()=='1':raise RuntimeError('Emulator cannot qualify physical-device gates')
  result['device']={'serial':serial,'sdk':command('-s',serial,'shell','getprop','ro.build.version.sdk').strip(),'fingerprint':command('-s',serial,'shell','getprop','ro.build.fingerprint').strip()}
  apk=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
  if not apk.is_file():raise RuntimeError('Debug APK unavailable')
  digest=hashlib.sha256(apk.read_bytes()).hexdigest();manifest=ROOT/'evidence/qualification/artifact-checksums.txt'
  expected=digest+'  app/build/outputs/apk/debug/app-debug.apk'
  if not manifest.is_file() or expected not in manifest.read_text().splitlines():raise RuntimeError('APK does not match current qualified checksum manifest')
  result['apkSha256']=digest
  if args.install:
   output=command('-s',serial,'install','-r',str(apk));result['automatedActions'].append({'action':'adb install -r','status':'PASS' if 'Success' in output else 'FAIL'})
   if 'Success' not in output:raise RuntimeError('APK installation not confirmed')
   command('-s',serial,'shell','am','start','-W','-n','com.aistudio.deproof.sdwk/com.example.MainActivity')
   result['automatedActions'].append({'action':'am start -W','status':'PASS','limitation':'User must inspect launched UI; launch intent success does not qualify rendering.'})
  result['preflight']='PASS';result['reason']='Preflight completed; record manual observations using docs/device-qualification.md. Security gates remain NOT_RUN.'
 except RuntimeError as e:result['preflight']='BLOCKED';result['reason']=str(e)
 return result
if __name__=='__main__':
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--adb');parser.add_argument('--serial');parser.add_argument('--install',action='store_true',help='Install/upgrade qualified APK and send launch intent; never clear data or uninstall.')
 args=parser.parse_args()
 result=run(args);dest=ROOT/'evidence/qualification/device-qualification.json';dest.parent.mkdir(parents=True,exist_ok=True)
 if dest.exists():
  previous=dest.with_name(dest.stem+'-'+datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')+'.json');dest.rename(previous)
 dest.write_text(json.dumps(result,indent=2)+'\n');print(result['reason']);print('Device gates: NOT_RUN. Evidence: '+str(dest.relative_to(ROOT)))
