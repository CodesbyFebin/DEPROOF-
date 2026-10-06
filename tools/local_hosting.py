#!/usr/bin/env python3
"""One reviewed, network-isolated local HTTP fixture. No arbitrary image/command/mount.
The Docker engine and local VM are owner-trusted; rootless engine is NOT claimed.
"""
import argparse,fcntl,json,pathlib,subprocess,time,sys
from verify import canonical,digest,require,load
IMAGE='busybox@sha256:73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662'
PROFILE={'schema':'deproof-service-profile-v1','id':'isolated-http-v1','image':IMAGE,'cpuQuotaMicros':'50000','memoryBytes':'33554432','pidLimit':'16','tmpfsBytes':'1048576','network':'none','ports':[],'mounts':[],'user':'65534:65534','readonlyRoot':True,'capabilities':[],'health':'CONTAINER_LOOPBACK_HTTP_8080; NO_HOST_PORT_EXPOSURE'}
FLAGS=['--user','65534:65534','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--network','none','--pids-limit','16','--memory','32m','--cpus','0.5','--tmpfs','/tmp:rw,noexec,nosuid,size=1m']
def docker(*args):return subprocess.run(['docker',*args],capture_output=True,text=True,timeout=20)
def qualified(evidence):
 report=load(pathlib.Path(evidence).read_bytes());require(report['status']=='PASS' and report['image']==IMAGE and time.time()-report['checkedAt']<86400,'RUNTIME_ISOLATION_UNQUALIFIED')
 r=docker('info','--format','{{json .}}');require(r.returncode==0,'RUNTIME_UNAVAILABLE');info=json.loads(r.stdout)
 require(info['ID']==report['runtimeId'] and info['ServerVersion']==report['serverVersion'] and info['OSType']=='linux','RUNTIME_CHANGED_REQUALIFY')
def inspect(name):
 r=docker('inspect',name)
 return json.loads(r.stdout)[0] if r.returncode==0 else None

def validate_container(value):
 require(value['Config']['Image']==IMAGE,'CONTAINER_IDENTITY_CHANGED')
 host=value['HostConfig'];require(host['ReadonlyRootfs'] and host['NetworkMode']=='none' and host['Memory']==33554432 and host['PidsLimit']==16 and host['NanoCpus']==500000000 and not host['Binds'] and not host['PortBindings'],'CONTAINER_BOUNDARY_CHANGED')
 require(value['Config']['User']=='65534:65534' and host['CapDrop']==['ALL'] and 'no-new-privileges' in host['SecurityOpt'],'CONTAINER_PRIVILEGES_CHANGED')
def status(name):
 value=inspect(name)
 if value is None:return {'observedState':'ABSENT','healthy':None,'containerId':None,'serviceCount':'0'}
 validate_container(value);running=value['State']['Running'];health=None
 if running:health=docker('exec',name,'wget','-q','-O','-','http://127.0.0.1:8080/').returncode==0
 return {'observedState':value['State']['Status'].upper(),'healthy':health,'containerId':value['Id'],'serviceCount':'1' if running else '0','startedAt':value['State']['StartedAt'],'finishedAt':value['State']['FinishedAt']}
def run(action,evidence,state,node,profile_digest,consent):
 if action=='start':qualified(evidence)
 require(len(node)==64 and all(c in '0123456789abcdef' for c in node),'NODE_ID_DENIED')
 root=pathlib.Path(state);name='deproof-service-'+node[:16]
 with (root/'service-owner.lock').open('a') as lock:
  fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
  if action=='start':
   require(consent and profile_digest==digest(canonical(PROFILE)),'SERVICE_PROFILE_CONSENT_REQUIRED')
   existing=inspect(name)
   if existing is not None:
    validate_container(existing)
    if not existing['State']['Running']:require(docker('start',name).returncode==0,'SERVICE_START_FAILED')
   else:
    # Fixed reviewed command, never constructed from remote user text.
    script="mkdir /tmp/site && printf '%s\\n' 'DEPR / Deproof — Built by CodesbyFebin' > /tmp/site/index.html && exec httpd -f -p 8080 -h /tmp/site"
    require(docker('run','-d','--name',name,*FLAGS,IMAGE,'sh','-c',script).returncode==0,'SERVICE_START_FAILED')
  if action=='stop':
   existing=inspect(name)
   if existing is not None:validate_container(existing);require(docker('stop','--time','2',name).returncode==0,'SERVICE_STOP_FAILED')
  result=status(name);result.update({'profile':PROFILE,'profileDigest':digest(canonical(PROFILE)),'observedAt':time.time(),'assurance':'QUALIFIED_LOCAL_VM_PROFILE; NOT_PUBLIC_HOSTING_OR_ROOTLESS_ENGINE'})
  if action=='stop':require(result['observedState'] in ('ABSENT','EXITED'),'STOP_TERMINATION_NOT_OBSERVED')
  tmp=root/'service-runtime.tmp';tmp.write_text(json.dumps(result));tmp.replace(root/'service-runtime.json');return result

def main():
 p=argparse.ArgumentParser();p.add_argument('action',choices=['start','stop','status']);p.add_argument('--evidence',required=True);p.add_argument('--state',required=True);p.add_argument('--node',required=True);p.add_argument('--profile-digest',default='');p.add_argument('--explicit-consent',action='store_true');a=p.parse_args()
 print(json.dumps(run(a.action,a.evidence,a.state,a.node,a.profile_digest,a.explicit_consent)))
if __name__=='__main__':main()
