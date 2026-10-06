#!/usr/bin/env python3
"""Summarize recorded container evidence without converting missing runs into passes."""
import datetime,hashlib,json,pathlib,re
ROOT=pathlib.Path(__file__).resolve().parents[1];OUT=ROOT/'evidence/docker';OUT.mkdir(parents=True,exist_ok=True)
now=datetime.datetime.now(datetime.timezone.utc).isoformat()
images=[]
for line in (OUT/'image-inspect.jsonl').read_text().splitlines():
 d=json.loads(line);images.append({'id':d['Id'],'tags':d.get('RepoTags',[]),'architecture':d['Architecture'],'os':d['Os'],'user':d['Config'].get('User','')})
integ=json.loads((OUT/'integration-report.json').read_text())
android=[]
for p in sorted((OUT/'android').glob('*/results.txt')):
 android.append({'run':p.parent.name,'results':p.read_text().splitlines(),'commands':(p.parent/'commands.txt').read_text().splitlines() if (p.parent/'commands.txt').exists() else [],'apkProduced':(p.parent/'app-debug.apk').is_file(),'logs':[str(x.relative_to(ROOT)) for x in p.parent.glob('*.log')]})
pins={str(p.relative_to(ROOT)):re.findall(r'^FROM ([^\s]+)',p.read_text(),re.M) for p in (ROOT/'docker').glob('*.Dockerfile')}
summary={'schema':'deproof-container-qualification-v1','generatedAt':now,'status':'BLOCKED','architecture':'Linux/amd64 on owner-trusted rootful Colima; Mac Intel host','product':'Deproof','mark':'DEPR','credit':'Built by CodesbyFebin','pins':pins,'images':images,'androidRuns':android,'integration':integ,'gates':[
 {'id':'compose-and-shell-validation','status':'PASS','commands':['docker compose config --quiet','bash -n docker/android-entrypoint.sh scripts/docker-build.sh scripts/docker-qualify.sh scripts/docker-up.sh scripts/docker-down.sh']},
 {'id':'service-images-build','status':'PASS','evidence':['evidence/docker/services-build-private-state.log'],'scope':'Node/prover/web images built. Final source changes require rebuild; not a runtime integration pass.'},
 {'id':'portable-container-qualification','status':'PASS','command':'bash scripts/docker-qualify.sh portable','evidence':['evidence/docker/portable-retry.log'],'scope':'Prior image: 18 Python tests; actual local Groth16 verification and tampered public input/proof rejection; 336 registry IDs. Newly added workflow tests are not container-qualified.'},
 {'id':'android-container-build','status':'BLOCKED','evidence':['evidence/docker/android-startup-trace.log'],'reason':'Observed Gradle cache lock directory failure from prior UID cache. UID-specific volumes now configured but untested; Docker VM disk is full. No container APK produced.'},
 {'id':'container-service-integration','status':integ['status'],'evidence':['evidence/docker/integration-report.json'],'reason':integ.get('failure')},
 {'id':'fixed-hosting-profile','status':'PASS','command':'python3 scripts/qualify-hosting.py','evidence':['evidence/qualification/hosting-isolation.json'],'scope':'Existing fixed profile on this runtime only. Revised separate owner-control image/profile not qualified.'},
 {'id':'revised-owner-control-profile','status':'BLOCKED','reason':'Earlier owner target built, but final root-owned separate state/report copy revisions require rebuild and exercise; storage unavailable.'},
 {'id':'shutdown-state-retention','status':'PASS','command':'bash scripts/docker-down.sh','scope':'Only Deproof development containers stopped, named volumes retained; fixture integration also stops its own project without volume deletion.'},
 {'id':'completion-kit','status':'FAIL','command':'MAX_ROUNDS=3 env -u DEPROOF_MODEL bash <external-completion-kit>/deproof-complete.sh build','evidence':['evidence/completion/20261006T071823Z-66439/round-1-report.md','evidence/completion/20261006T071823Z-66439/logs/qualification-1.log'],'reason':'Inner sandbox denied process inspection of launcher-held lock; no source changes or completion plan. Launcher ran all 14 host gates successfully then exited 1. Rounds 2/3 not run.'}],
 'blockers':[{'id':'STORAGE','status':'BLOCKED','reason':'Current read-only probe: Docker VM /dev/vdb1 20G, zero available (100%); host 4.1GiB available. See evidence/docker/storage-current.json. No user images, caches, volumes or artifacts deleted; no shared VM restart performed.'},{'id':'PHYSICAL_DEVICE','status':'NOT_RUN','reason':'No available physical device; container/emulator cannot qualify wallet handoff or hardware keys.'},{'id':'PRODUCTION_PROOF_EXTERNAL_RELEASE','status':'BLOCKED','reason':'Local educational proof, owner-trusted hosting and development container results do not qualify production proofs, external providers, asset rights, release signing or deployment.'}],
 'registriesPreserved':{'F':120,'C':100,'FN':62,'EF':24,'E':30,'totalEntries':336},'performedMainnetSpending':False,'publishedOrDeployed':False}
(OUT/'qualification-summary.json').write_text(json.dumps(summary,indent=2)+'\n')
lines=['# Container qualification','',f'Generated: {now}. Deproof / DEPR — Built by CodesbyFebin.','', '**BLOCKED**: container images and portable checks succeeded; end-to-end container qualification is incomplete.','', '| Gate | Status | Scope / reason |','|---|---|---|']
for g in summary['gates']:lines.append('| '+g['id']+' | '+str(g['status'])+' | '+str(g.get('scope',g.get('reason','Recorded commands and evidence in qualification-summary.json.')))+' |')
lines+=['','No container APK was produced. The host-built APK and all prior checkpoints are preserved.','', 'Next: provide host/VM storage, rerun Android/services builds, portable/integration checks and owner-profile qualification. Final source changes are not yet container-qualified. Keep all 336 registry IDs and unresolved device/external acceptance gates.','', 'Commands and usage: [docker-development.md](../../docs/docker-development.md). No publishing, deployment, token issuance or mainnet spending.']
(OUT/'qualification-report.md').write_text('\n'.join(lines)+'\n')
print('Recorded scoped container qualification and actual blockers.')
