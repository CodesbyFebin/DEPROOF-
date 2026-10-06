#!/usr/bin/env python3
"""Controlled, owner-local hostile fixtures against a pinned cached image.
The Docker engine/VM is trusted; nonroot workload restrictions are tested separately.
"""
import json,pathlib,subprocess,sys,time,uuid
ROOT=pathlib.Path(__file__).resolve().parents[1]
IMAGE='busybox@sha256:73aaf090f3d85aa34ee199857f03fa3a95c8ede2ffd4cc2cdb5b94e566b11662'
FLAGS=['--user','65534:65534','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--network','none','--pids-limit','16','--memory','32m','--cpus','0.5','--tmpfs','/tmp:rw,noexec,nosuid,size=1m']
def docker(*args,timeout=30):return subprocess.run(['docker',*args],capture_output=True,text=True,timeout=timeout)
def main():
 out=ROOT/'evidence/qualification/hosting-isolation.json';checks=[]
 info=docker('info','--format','{{json .}}')
 if info.returncode:
  out.write_text(json.dumps({'schema':'deproof-host-isolation-v1','status':'BLOCKED','reason':'DOCKER_RUNTIME_UNAVAILABLE','detail':'Runtime probe failed; inspect host-runtime-probe.log','checks':[]}));return 1
 runtime=json.loads(info.stdout)
 if runtime.get('OSType')!='linux' or runtime.get('CgroupVersion')!='2':
  out.write_text(json.dumps({'schema':'deproof-host-isolation-v1','status':'BLOCKED','reason':'UNSUPPORTED_LINUX_CGROUP_RUNTIME','checks':[]}));return 1
 inspected=docker('image','inspect',IMAGE,'--format','{{json .RepoDigests}}')
 if inspected.returncode:
  out.write_text(json.dumps({'schema':'deproof-host-isolation-v1','status':'BLOCKED','reason':'PINNED_IMAGE_NOT_CACHED','image':IMAGE,'checks':[]}));return 1
 fixtures={
 'nonroot_identity':'test "$(id -u)" = 65534',
 'readonly_root':'if touch /deproof-root-write 2>/dev/null; then exit 1; fi',
 'capabilities_dropped':"grep -q '^CapEff:[[:space:]]*0000000000000000$' /proc/self/status",
 'no_new_privileges':"grep -q '^NoNewPrivs:[[:space:]]*1$' /proc/self/status",
 'network_has_no_route':"test $(wc -l < /proc/net/route) -eq 1",
 'memory_cap_enforced':'test "$(cat /sys/fs/cgroup/memory.max)" = 33554432',
 'cpu_cap_enforced':'test "$(cat /sys/fs/cgroup/cpu.max)" = "50000 100000"',
 'cpu_throttling_observed':"(while :; do :; done) & child=$!; sleep 2; kill $child; awk '/throttled_usec/ {exit !($2>0)}' /sys/fs/cgroup/cpu.stat",
 'pid_cap_configured':'test "$(cat /sys/fs/cgroup/pids.max)" = 16',
 'tmpfs_noexec':'printf "#!/bin/sh\\nexit 0\\n" > /tmp/probe; chmod +x /tmp/probe; if /tmp/probe 2>/dev/null; then exit 1; fi',
 'tmpfs_disk_exhaustion':'if dd if=/dev/zero of=/tmp/fill bs=1048576 count=2 2>/dev/null; then exit 1; fi',
 'pid_exhaustion':'for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do sleep 1 & done; wait'
 }
 for name,script in fixtures.items():
  run=docker('run','--rm',*FLAGS,IMAGE,'sh','-c',script,timeout=15)
  # A pid bomb is bounded to 16 tasks; the shell itself fails at fork under the enforced cgroup.
  passed=run.returncode==0 if name!='pid_exhaustion' else run.returncode!=0 and ('Resource temporarily unavailable' in run.stderr or 'fork' in run.stderr)
  checks.append({'id':name,'status':'PASS' if passed else 'FAIL','exitCode':run.returncode,'stderr':run.stderr[:1000]})
  print(('PASS' if passed else 'FAIL')+': '+name,flush=True)
 name='deproof-memory-fixture-'+uuid.uuid4().hex[:12]
 try:
  run=docker('run','--name',name,*FLAGS,IMAGE,'awk','BEGIN {for(i=0;i<1000000;i++) a[i]=sprintf("%01024d", i); print "UNEXPECTED"}',timeout=15)
  state=docker('inspect',name,'--format','{{json .State}}');observed=json.loads(state.stdout)
  checks.append({'id':'memory_exhaustion_oom_observed','status':'PASS' if observed.get('OOMKilled') and run.returncode!=0 else 'FAIL','exitCode':run.returncode,'oomKilled':observed.get('OOMKilled')})
 finally:docker('rm','-f',name)
 report={'schema' :'deproof-host-isolation-v1','status':'PASS' if all(x['status']=='PASS' for x in checks) else 'FAIL','runtimeId':runtime.get('ID'),'runtimeOS':runtime.get('OperatingSystem'),'serverVersion':runtime.get('ServerVersion'),'securityOptions':runtime.get('SecurityOptions'),'engineProtection':'OWNER_TRUSTED_ROOTFUL_ENGINE_IN_LOCAL_LINUX_VM; NOT_ROOTLESS','image':IMAGE,'checkedAt':time.time(),'checks':checks,'limitations':['Qualifies this constrained nonroot container profile on this local Linux VM only.','Does not qualify arbitrary services, network exposure, peer tunnels or trusted production proof execution.']}
 out.write_text(json.dumps(report,indent=2)+'\n');return 0 if report['status']=='PASS' else 1
if __name__=='__main__':sys.exit(main())
