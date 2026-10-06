#!/usr/bin/env python3
"""Map tested local slices without treating overlapping/device requirements as complete."""
import json,pathlib,re,collections
r=pathlib.Path(__file__).resolve().parents[1];out=r/'evidence/qualification'
def ids(text):
 found=[]
 for match in re.finditer(r'\b(FN|EF|F|C|E)(\d{3})(?:[–-](?:(FN|EF|F|C|E))?(\d{3}))?',text or ''):
  prefix,start,other,end=match.groups();found.extend(f'{prefix}{n:03}' for n in range(int(start),int(end or start)+1))
 return sorted(set(found))
def passed(gate):
 file=out/'command-results.jsonl'
 return file.exists() and any(json.loads(x)['gate']==gate and json.loads(x)['exitCode']==0 for x in file.read_text().splitlines())
docs={n:json.loads((r/(n+'.json')).read_text()) for n in ['features','core-checks','functions','ecosystem-requirements']}
features=docs['features']['features'];checks=docs['core-checks']['checks'];functions=docs['functions']['base']+docs['functions']['ecosystem'];requirements=docs['ecosystem-requirements']['requirements']
byid={x['id']:x for x in features+checks+functions+requirements}
for c in checks:c['featureIds']=ids(c.get('featureOverlap',''))
for e in requirements:
 e['featureIds']=ids(e.get('baseMappingAndPhase',''));e['checks']=ids(e.get('coreCheckOverlap','')) or [e['acceptanceId']];e['contractIds']=ids(e.get('contracts',''))
fn_features={1:[2],2:[4],3:[7],4:list(range(1,11)),5:[40,41],6:[28,45],7:[35,40],8:list(range(31,41)),9:[33],10:[39],11:[32],12:[45],13:[45],14:[48],15:[48],16:[45,46],17:[47,48],18:[41],19:[41],20:[42],21:[4,7],22:[79],23:[5,6,7],24:[19,20],25:[19],26:[12,13,14],27:[15],28:[76,80],29:[50,75],30:[80],31:[63],32:[61,62],33:[67],34:[70],35:[79],36:[87],37:[18],38:[39],39:[45,46],40:[80],41:[28],42:[28,40],43:[40],44:[44],45:[44,98],46:[35,36],47:[32,36,48],48:[49,97],49:[20,78],50:[20,48],51:[71],52:[59,74],53:[64,70],54:list(range(75,81)),55:[79],56:[81],57:[82],58:[85,88],59:list(range(101,111)),60:[120],61:[99],62:[67,68,69,70]}
for f in functions:
 if f['id'].startswith('FN'):f['featureIds']=[f'F{x:03}' for x in fn_features[int(f['id'][2:])]]
 else:
  f['requirementIds']=[e['id'] for e in requirements if f['id'] in e['contractIds']]
  f['featureIds']=sorted({x for eid in f['requirementIds'] for x in byid[eid]['featureIds']})
node=['app/src/main/java/com/example/NodeWorkspace.kt','app/src/main/java/com/example/data/NodeRepository.kt','app/src/main/java/com/example/data/NodeClient.kt','app/src/main/java/com/example/domain/NodeProtocol.kt','node-agent/internal/agent/agent.go']
host=node+['node-agent/internal/agent/hosting.go','tools/local_hosting.py','scripts/qualify-hosting.py']
proof=node+['node-agent/internal/agent/proof.go','tools/proof_jobs.py','prover-worker/cmd/prove/main.go','prover-worker/cmd/verify/main.go']
new_slices={
 'node-integration':(['E011','E016','E017','E018','E019','E021','E023','E024','E025','E030','EF002','EF003','EF010','EF011','EF012','EF013','EF014','EF015','EF016','EF018','EF020','EF021','EF022','F104','F108'],node+proof,['scripts/qualify-integration.py','app/src/test/java/com/example/domain/NodeProtocolTest.kt'],['evidence/qualification/node-integration.log','evidence/qualification/node-integration.json']),
 'hosting-isolation':(['E012','E013','E014','EF005','EF006','EF007','EF008'],host,['scripts/qualify-hosting.py','scripts/qualify-integration.py'],['evidence/qualification/hosting-isolation.json','evidence/qualification/node-integration.json']),
 'rpc-read-only':(['F003','F008','E002','FN004','C003','C008'],['app/src/main/java/com/example/data/Rpc.kt'],['tools/jvm/LiveRpcProbe.kt','app/src/test/java/com/example/domain/NodeProtocolTest.kt'],['evidence/qualification/live-rpc.json']),
 'verifier-tests':(['F060','F067','F079','F118','FN051','FN052','FN055'],['app/src/main/java/com/example/data/Store.kt','app/src/main/resources/db/migration_1_2.sql','app/src/main/java/com/example/MainActivity.kt'],['tools/test_migrations.py','app/src/test/java/com/example/domain/NodeProtocolTest.kt'],['evidence/qualification/verifier-tests.log']),
 'browser-checks':(['F119','F116'],['web/style.css','web/inspect.js','scripts/build-web.py'],['scripts/browser/qualify.mjs'],['evidence/qualification/browser/report.json'])}
for gate,(entries,paths,tests,evidence) in new_slices.items():
 if not passed(gate):continue
 for id in entries:
  e=byid[id];e['implementationPaths']=sorted(set(e['implementationPaths']+paths));e['tests']=sorted(set(e['tests']+tests));e['evidencePaths']=sorted(set(e['evidencePaths']+evidence))
  if not id.startswith('C') and e['status'] not in ('VERIFIED',):e['status']='IMPLEMENTED_UNVERIFIED'
  e['qualificationDimensions']=['VERIFIED_LOCAL','NOT_RUN']
  e['limitations']=['Named local slice passed; complete authoritative acceptance (including applicable device/external/platform gates) remains incomplete. See uncovered-requirements.json.']
if passed('android-build'):
 e=byid['C100'];e.update(status='PASS',implementationPaths=['app/build.gradle.kts','gradlew'],tests=['scripts/qualify-android.sh'],evidencePaths=['evidence/qualification/android-build.log'],qualificationDimensions=['VERIFIED_LOCAL'],limitations=['Debug assembly/JVM tests/lint only; no installed-device, wallet or release qualification.'])
# Scoped contracts that the actual TLS/socket and isolated local-container tests fully exercise.
for id in ['EF002','EF003','EF006','EF007','EF010','EF014','EF015','EF021','EF022']:
 if passed('node-integration') and (id not in ['EF006','EF007'] or passed('hosting-isolation')):
  byid[id]['status']='VERIFIED';byid[id]['qualificationDimensions']=['VERIFIED_LOCAL'];byid[id]['limitations']=['Verified only for the documented owner-local loopback/cubic/isolated HTTP profile; not Android Keystore, physical device, arbitrary hosting or external protocol qualification.']
if passed('core-tests'):
 for id,paths,tests,reason in [
  ('F058',['app/src/main/java/com/example/WorkflowWorkspace.kt','app/src/main/java/com/example/data/LocationCapture.kt','app/src/main/java/com/example/domain/Workflows.kt','app/src/main/java/com/example/domain/Records.kt','tools/verify.py','contracts/location-observation-v1.schema.json'],['app/src/test/java/com/example/domain/WorkflowTest.kt::locationConsentDenialAndProvenance','app/src/test/java/com/example/domain/WorkflowTest.kt::crossLanguageOptionalLocationVectorAndMutation','tools/test_location.py'],'Consented one-shot foreground approximate location and optional manifest extension implemented; typed freshness/range/provenance and cross-language hash checks pass. Real permission/provider/cancellation/revocation and Android rendering/lifecycle remain NOT_RUN; no physical-truth assertion.'),
  ('F105',['app/src/main/java/com/example/WorkflowWorkspace.kt','app/src/main/java/com/example/domain/Workflows.kt','app/src/main/java/com/example/data/Store.kt'],['app/src/test/java/com/example/domain/WorkflowTest.kt::offlineMappingPlanRoundTripAndBounds','tools/test_migrations.py::test_new_private_workflow_drafts_survive_reopen_and_replace_atomically'],'Offline ordered user-coordinate planner is persisted and explicitly exported; invalid edits do not replace the saved draft. No route provider, coverage/rewards or observed travel. Android editing/export/reopen/accessibility remain NOT_RUN.'),
  ('F115',['app/src/main/java/com/example/WorkflowWorkspace.kt','app/src/main/java/com/example/data/ReminderRepository.kt','app/src/main/java/com/example/domain/Workflows.kt','app/src/main/AndroidManifest.xml'],['app/src/test/java/com/example/domain/WorkflowTest.kt::reminderConsentPermissionClockAndReplacement','tools/test_migrations.py::test_new_private_workflow_drafts_survive_reopen_and_replace_atomically'],'Explicit opt-in, permission-aware inexact scheduling, replacement generations, cancellation/global disable and boot/clock/startup recovery implemented. Local state tests do not establish actual notification delivery, Android lifecycle or provider cooldown eligibility; these remain NOT_RUN. POST_REQUESTED is not delivered.')]:
  byid[id].update(status='IMPLEMENTED_UNVERIFIED',implementationPaths=paths,tests=tests,evidencePaths=['evidence/qualification/core-tests.log','evidence/qualification/verifier-tests.log','evidence/qualification/device-qualification.json'],qualificationDimensions=['VERIFIED_LOCAL','NOT_RUN'],limitations=[reason])
 byid['F093'].update(status='IMPLEMENTED_UNVERIFIED',implementationPaths=['app/src/main/java/com/example/domain/Explanations.kt','app/src/main/java/com/example/MainActivity.kt'],tests=['app/src/test/java/com/example/domain/NodeProtocolTest.kt::accountEffectsDecodeOnlyExactReadOnlyCreateAccount'],evidencePaths=['evidence/qualification/core-tests.log'],qualificationDimensions=['VERIFIED_LOCAL','NOT_RUN'],limitations=['Read-only exact System CreateAccount layout and policy-approved memo/TransferChecked effects only. Unknown layouts remain unknown/refused; no rent minimum, execution or broader account-creation claim. Android rendering remains NOT_RUN.'])
 byid['FN061'].update(status='BLOCKED',implementationPaths=['app/src/main/java/com/example/domain/Explanations.kt','app/src/main/java/com/example/MainActivity.kt'],tests=['app/src/test/java/com/example/domain/NodeProtocolTest.kt::stagedExplanationNeverTransmitsOrChangesAuthorization'],evidencePaths=['evidence/qualification/core-tests.log'],qualificationDimensions=['VERIFIED_LOCAL','BLOCKED'],limitations=['Only configuration/consent/blocked-adapter error paths are implemented and tested; no qualified provider adapter or outbound request exists. External provider integration remains blocked, not complete. Authorization unchanged.'])
 local_contracts={
  'F055':(['app/src/main/java/com/example/EvidencePreview.kt','app/src/main/java/com/example/domain/LocalEvidence.kt','app/src/main/java/com/example/MainActivity.kt'],['app/src/test/java/com/example/domain/NodeProtocolTest.kt::privatePreviewChecksBytesPathsEncodingAndTypes'],'Only bounded text/plain, Markdown, JSON, JPEG/PNG/WebP previews implemented; unsupported types explicitly refused. Actual Android rendering/accessibility/lifecycle remains NOT_RUN.'),
  'F095':(['app/src/main/java/com/example/MainActivity.kt','app/src/main/java/com/example/domain/LocalEvidence.kt'],['app/src/test/java/com/example/domain/NodeProtocolTest.kt::legacyDigestAndFirstSeenIndicatorsNeverGrantTrust'],'Local first-seen classification tested; Android DataStore persistence, rendering and lifecycle remain NOT_RUN. Indicator never changes policy.'),
  'F097':(['app/src/main/java/com/example/MainActivity.kt','app/src/main/java/com/example/domain/Solana.kt'],['app/src/test/java/com/example/domain/CoreTest.kt::completeMessageMutationsNeverRetainApproval'],'Existing memo rebuild clears fee approval and requires fresh review; byte/context mutation refusal tested. Expired-blockhash device/wallet flow and exactly-one retry observation remain NOT_RUN.'),
  'FN048':(['app/src/main/java/com/example/MainActivity.kt','app/src/main/java/com/example/domain/Solana.kt'],['app/src/test/java/com/example/domain/CoreTest.kt::completeMessageMutationsNeverRetainApproval'],'Existing one-rebuild memo implementation and local binding test; device handoff/reapproval remains NOT_RUN.'),
  'FN033':(['app/src/main/java/com/example/data/EvidenceSigner.kt','app/src/main/java/com/example/domain/LocalEvidence.kt'],['app/src/test/java/com/example/domain/NodeProtocolTest.kt::legacyDigestAndFirstSeenIndicatorsNeverGrantTrust','app/src/test/java/com/example/domain/NodeProtocolTest.kt::legacySignatureUsesRawDigestBytesNotHexOrBase64'],'32 raw bytes, separate legacy alias, DER/SPKI and hardware refusal implemented; local raw/size/signature tests pass. Actual Android hardware-key generation, KeyInfo and signature remain NOT_RUN; no legacy-import workflow claimed.')}
 for id,(paths,tests,reason) in local_contracts.items():
  byid[id].update(status='IMPLEMENTED_UNVERIFIED',implementationPaths=paths,tests=tests,evidencePaths=['evidence/qualification/core-tests.log','evidence/qualification/device-qualification.json'],qualificationDimensions=['VERIFIED_LOCAL','NOT_RUN'],limitations=[reason])
if passed('localization-catalogs'):
 byid['F117'].update(status='IMPLEMENTED_UNVERIFIED',implementationPaths=['app/src/main/java/com/example/LanguageControls.kt','app/src/main/java/com/example/MainActivity.kt','app/src/main/res/values/strings.xml','app/src/main/res/values-es/strings.xml'],tests=['scripts/qualify-localization.py'],evidencePaths=['evidence/qualification/localization-report.json','evidence/qualification/localization-catalogs.log'],qualificationDimensions=['VERIFIED_LOCAL','NOT_RUN'],limitations=['204 English/Spanish catalog strings have matching IDs/formats and waypoint plural rules; persisted UI language switching implemented. Full dynamic decoder/account-effect and remaining fallback localization, independent translation review and device layouts/accessibility/restart remain incomplete. Catalog parity is not complete F117 acceptance.'])
for f in features:
 f['checks']=[c['id'] for c in checks if f['id'] in c['featureIds']]
 f['contractIds']=[x['id'] for x in functions if f['id'] in x['featureIds']]
for n,d in docs.items():(r/(n+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
uncovered=[]
for e in byid.values():
 for p in e.get('implementationPaths',[]):assert (r/p).exists(),(e['id'],p)
 for test in e.get('tests',[]):assert (r/test.split('::')[0]).exists(),(e['id'],test)
 for p in e.get('evidencePaths',[]):assert (r/p).is_file(),(e['id'],p)
 if e['status'] not in ['VERIFIED','PASS']:
  uncovered.append({'id':e['id'],'status':e['status'],'acceptance':e.get('acceptance') or e.get('definition'),'implementedPaths':e.get('implementationPaths',[]),'namedTests':e.get('tests',[]),'requiredUncoveredGate':e['limitations']})
report={'schema':'deproof-uncovered-requirements-v1','exactCounts':{'F':120,'C':100,'FN':62,'EF':24,'E':30},'uncoveredCount':len(uncovered),'requirements':uncovered,'coverageCounts':{n:dict(collections.Counter(e['status'] for e in (d['base']+d['ecosystem'] if n=='functions' else d['features'] if n=='features' else d['checks'] if n=='core-checks' else d['requirements']))) for n,d in docs.items()}}
(out/'uncovered-requirements.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
print('PASS: all 336 registry entries reconciled; '+str(len(uncovered))+' remain outside complete acceptance. No overlaps added as unique features.')
