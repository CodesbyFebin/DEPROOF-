#!/usr/bin/env python3
"""Record actual build inputs and Git dirt without claiming HEAD alone was tested."""
import datetime,hashlib,json,pathlib,subprocess,sys
root=pathlib.Path(__file__).resolve().parents[1]
def git(*args):
 p=subprocess.run(['git',*args],cwd=root,capture_output=True)
 return p.stdout.decode(errors='replace').strip() if p.returncode==0 else None
files=set()
for base in ['app/src','app/schemas','contracts','fixtures','tools','scripts','docker','web','node-agent/cmd','node-agent/internal','prover-worker/cmd','prover-worker/internal','.github','docs','assets']:
 files.update(p for p in (root/base).rglob('*') if p.is_file() and not any(x in p.parts for x in ['node_modules','__pycache__']))
for name in ['app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties','node-agent/go.mod','prover-worker/go.mod','prover-worker/go.sum','compose.yaml','.dockerignore','.gitignore','features.json','core-checks.json','functions.json','ecosystem-requirements.json','README.md','LICENSE']:
 p=root/name
 if not p.is_file():raise SystemExit('Required source input missing: '+name)
 files.add(p)
records={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(files)}
payload=json.dumps(records,sort_keys=True,separators=(',',':')).encode()
result={'schema':'deproof-source-manifest-v1','recordedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'checkout':str(root),'gitRoot':git('rev-parse','--show-toplevel'),'branch':git('branch','--show-current'),'head':git('rev-parse','HEAD'),'gitStatus':git('status','--porcelain=v1','--untracked-files=all'),'trackedChangesSha256':hashlib.sha256(subprocess.run(['git','diff','HEAD','--binary'],cwd=root,capture_output=True,check=True).stdout).hexdigest(),'sourceSetSha256':hashlib.sha256(payload).hexdigest(),'files':records,'limits':['Actual working-tree inputs; commit alone is not tested-source identity.','Generated qualification, web and registry outputs may change during the suite; compare pre/post manifests.','Private state, local.properties, build caches and signing keys excluded.']}
dest=pathlib.Path(sys.argv[1] if len(sys.argv)>1 else 'evidence/qualification/source-manifest.json');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(result,indent=2)+'\n');print('Recorded',len(records),'source hashes at',dest)
