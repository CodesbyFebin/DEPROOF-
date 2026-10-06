#!/usr/bin/env python3
"""Evidence mappings are deliberately conservative: partial UI/protocol work is unverified."""
import pathlib,json,re
root=pathlib.Path(__file__).resolve().parents[1]
# Criterion-specific test links are authoritative and survive regeneration.
CRITERION_LINKS=json.loads((root/'scripts/criterion-test-links.json').read_text(encoding='utf-8'))['links']
for _id,_refs in CRITERION_LINKS.items():
 for _ref in _refs:
  _path,_name=_ref.split('::')
  _src=(root/_path).read_text(encoding='utf-8')
  assert re.search(r'(fun\s+|def\s+)'+re.escape(_name)+r'\s*\(',_src),f'criterion link {_id} names missing test {_ref}'
D='app/src/main/java/com/example/domain/';A='app/src/main/java/com/example/';N='node-agent/internal/agent/agent.go'
core_test='app/src/test/java/com/example/domain/CoreTest.kt';core_log='evidence/qualification/core-tests.log';node_log='evidence/qualification/node-tests.log';portable_log='evidence/qualification/verifier-tests.log'
feature_paths={}
def assign(ids,paths):
 for i in ids:feature_paths[f'F{i:03}']=paths
assign(range(1,11),[D+'Core.kt',A+'data/Rpc.kt',A+'MainActivity.kt'])
assign(range(11,21),[A+'wallet/Wallet.kt',D+'Solana.kt',A+'MainActivity.kt'])
assign(range(21,31),[A+'data/Rpc.kt',D+'Solana.kt',A+'MainActivity.kt'])
assign(range(31,51),[D+'Core.kt',D+'Solana.kt',A+'MainActivity.kt'])
assign([51,52,53,54,56,57,59,60],[A+'MainActivity.kt',A+'data/Store.kt'])
assign([55],[A+'EvidencePreview.kt',D+'LocalEvidence.kt',A+'MainActivity.kt'])
assign([95],[A+'MainActivity.kt',D+'LocalEvidence.kt'])
assign([97],[A+'MainActivity.kt',D+'Solana.kt'])
assign([58],[A+'WorkflowWorkspace.kt',A+'data/LocationCapture.kt',D+'Workflows.kt',D+'Records.kt','tools/verify.py','contracts/location-observation-v1.schema.json'])
assign([105],[A+'WorkflowWorkspace.kt',A+'data/Store.kt',D+'Workflows.kt'])
assign([115],[A+'WorkflowWorkspace.kt',A+'data/ReminderRepository.kt',A+'data/Store.kt',D+'Workflows.kt'])
assign([117],[A+'LanguageControls.kt',A+'MainActivity.kt','app/src/main/res/values/strings.xml','app/src/main/res/values-es/strings.xml','scripts/qualify-localization.py'])
assign(range(61,67),[D+'Core.kt',D+'Records.kt','tools/verify.py'])
assign(range(67,71),[A+'data/EvidenceSigner.kt',D+'Records.kt','tools/verify.py'])
assign(range(71,81),[A+'data/Store.kt',A+'MainActivity.kt',D+'Records.kt'])
assign([91,92,94,96],[D+'Core.kt',D+'Solana.kt'])
assign([93],[D+'Explanations.kt',A+'MainActivity.kt'])
assign([98,99,100],[A+'data/Rpc.kt',A+'MainActivity.kt'])
assign([104,108],[N]);assign([109],[D+'Records.kt','docs/threat-model.md'])
assign([114],['docs/setup.md','docs/privacy.md']);assign([116],[A+'MainActivity.kt',A+'Theme.kt','web/style.css'])
assign([118],["tools/bundle.py","tools/test_bundle.py"])
assign([119],['web/index.html','web/inspect.js','scripts/build-web.py'])
assign([120],['scripts/qualify.sh','scripts/update-coverage.py','docs/setup.md'])
verified_f={2,4,33,61,62,63,65}
blocked_f=set(range(81,91))|{101,102,103,106,107,110,111,112,113}
fn_paths={}
for i in [1,2,3,8,9,10,11,12,13,14,18,19,20,21,32,38,43,46]:fn_paths[i]=[D+'Core.kt']
for i in [6,7,16,17,39,41,50]:fn_paths[i]=[D+'Solana.kt']
for i in [4,22,23,25,44,45,49]:fn_paths[i]=[A+'data/Rpc.kt']
for i in [24,26,27]:fn_paths[i]=[A+'wallet/Wallet.kt',A+'MainActivity.kt']
for i in [28,29,30,40,51,52,54,55]:fn_paths[i]=[A+'data/Store.kt',A+'MainActivity.kt',D+'Records.kt']
for i in [31,34]:fn_paths[i]=[D+'Records.kt','tools/verify.py']
fn_paths[53]=['tools/verify.py'];fn_paths[60]=['scripts/qualify.sh','scripts/update-coverage.py'];fn_paths[62]=[A+'data/EvidenceSigner.kt']
fn_paths[33]=[A+'data/EvidenceSigner.kt',D+'LocalEvidence.kt']
fn_paths[61]=[D+'Explanations.kt',A+'MainActivity.kt']
verified_fn={1,2,3,9,10,11,12,13,14,16,20,21,31,32,34,39,50,53}
# FN015 is explicitly test-only, never production tamper behavior.
fn_paths[5]=[D+"Helpers.kt"];fn_paths[35]=[D+"Helpers.kt"];fn_paths[36]=[D+"Helpers.kt"];fn_paths[37]=[D+"Helpers.kt"];fn_paths[47]=[D+"Helpers.kt"];fn_paths[48]=[A+"MainActivity.kt"]
fn_paths[15]=[core_test]
verified_fn.add(15)
local_checks={2,4,27,31,32,33,35,36,37,41,42,43,45,46,49,70,73,87,97}
check_tests={2:'addressesRejectAlphabetLengthAndOversize',4:'exactUnsignedVectorsAndDecimalFormatting',27:'exactUnsignedVectorsAndDecimalFormatting',70:'rawStreamingLimitCancellationAndCanonicalUnicode',73:'crossLanguageGoldenVectors',45:'memoParserAndImmutableMessageContextMutations',46:'memoParserAndImmutableMessageContextMutations',49:'memoParserAndImmutableMessageContextMutations'}
for name,key in [('features','features'),('core-checks','checks'),('functions','functions'),('ecosystem-requirements','requirements')]:
 p=root/(name+'.json');d=json.loads(p.read_text())
 for e in (d["base"] + d["ecosystem"] if name == "functions" else d[key]):
  previous_tests=list(e.get('tests',[]));id=e['id'];num=int(re.search(r'\d+',id).group());e.setdefault('dependencies',[]);e['checks']=[];e['tests']=[];e['implementationPaths']=[];e['evidencePaths']=[];e['qualificationDimensions']=[];e['limitations']=[]
  e['sourceMappings']={'master':'docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/DEPROOF_FINAL_MASTER_PROMPT.md','registry':'docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN/'+name+'.json','id':id}
  if id.startswith('F') and not id.startswith('FN'):
   phase='P1' if num<=50 or 61<=num<=70 or 75<=num<=80 or 91<=num<=100 else 'P2' if num<=80 else 'P3' if num<=110 else 'P4' if num in [119,120] else 'P5';e['phase']=phase
   paths=feature_paths.get(id,[]);e['implementationPaths']=paths
   e['status']='VERIFIED' if num in verified_f else 'BLOCKED' if num in blocked_f else 'IMPLEMENTED_UNVERIFIED' if paths else 'NOT_STARTED'
   if num in verified_f:e['tests']=[core_test+'::'+('crossLanguageGoldenVectors' if num==63 else 'addressesRejectAlphabetLengthAndOversize' if num==2 else 'exactUnsignedVectorsAndDecimalFormatting' if num in [4,33] else 'rawStreamingLimitCancellationAndCanonicalUnicode')];e['evidencePaths']=[core_log];e['qualificationDimensions']=['VERIFIED_LOCAL']
   elif paths:e['limitations']=['Partial implementation; required Android/device/runtime/provider gates remain unqualified.'];e['qualificationDimensions']=['NOT_RUN']
   else:e['limitations']=['Required behavior not implemented.']
   if num in blocked_f:e['limitations']=['Official protocol/provider, dependency or deployment prerequisites unavailable; no execution/qualification claim.'];e['qualificationDimensions']=['BLOCKED']
  elif id.startswith('C'):
   e['phase']='P1';e['status']='PASS' if num in local_checks else 'NOT_RUN';e['implementationPaths']=[D+'Core.kt',D+'Solana.kt',D+'Records.kt'];e['featureIds']=re.findall(r'F\d{3}',e.get('featureOverlap',''))
   if num in local_checks:e['tests']=[core_test+'::'+check_tests.get(num,'strictPolicyChecksAllRolesMintsOwnersAndSiblings')];e['evidencePaths']=[core_log];e['qualificationDimensions']=['VERIFIED_LOCAL'];e['limitations']=['Synthetic/local domain check; does not qualify device/network integration.']
   else:e['limitations']=['Full acceptance gate not executed; code presence/related tests do not imply PASS.'];e['qualificationDimensions']=['NOT_RUN']
  elif id.startswith('FN'):
   e['phase']='P2' if num in [51,52] else 'P3' if num in [56,57,58,59] else 'P4' if num==60 else 'P1';e['implementationPaths']=fn_paths.get(num,[]);e['featureIds']=[]
   e['status']='VERIFIED' if num in verified_fn else 'IMPLEMENTED_UNVERIFIED' if e['implementationPaths'] else 'BLOCKED' if num in [42,47,56,57,58,59] else 'NOT_STARTED'
   if num in verified_fn:e['tests']=[core_test] if num!=53 else ['tools/test_verifier.py'];e['evidencePaths']=[core_log] if num!=53 else [portable_log];e['qualificationDimensions']=['VERIFIED_LOCAL']
   e['limitations']=['Only supported local contract profile qualified; no inferred device/protocol qualification.'] if num in verified_fn else ['Full typed contract/required integration remains incomplete.']
  elif id.startswith('EF'):
   e['phase']='P3' if num!=23 else 'P4';e['status']='IMPLEMENTED_UNVERIFIED' if num in [1,2,3,4,10,11,12,13,14,15,16,20,21,23,24] else 'BLOCKED'
   e['implementationPaths']=[N] if num in [1,2,3,4,10,11,12,13,14,15] else ['tools/verify.py'] if num==16 else ['prover-worker/cmd/prove/main.go'] if num==20 else ['prover-worker/cmd/verify/main.go'] if num==21 else [A+'MainActivity.kt','tools/verify.py','tools/bundle.py'] if num==23 else ['scripts/qualify.sh'] if num==24 else []
   e['limitations']=['Partial local implementation; Android wiring, host/runtime/network/external qualification incomplete.'];e['qualificationDimensions']=['NOT_RUN'] if e['status']!='BLOCKED' else ['BLOCKED']
   if num in [1,2,3,4,10,14]:e['tests']=['node-agent/internal/agent/agent_test.go'];e['evidencePaths']=[node_log]
  else:
   e['status']='BLOCKED' if num in [8,9,10,12,13,15,20,21,22,23,24,25] else 'IMPLEMENTED_UNVERIFIED';e['implementationPaths']=([N] if 11<=num<=20 else ['prover-worker/cmd/prove/main.go','prover-worker/cmd/verify/main.go'] if 21<=num<=25 else [D+'Core.kt',D+'Solana.kt',D+'Records.kt',A+'MainActivity.kt']);e['qualificationDimensions']=['BLOCKED'] if e['status']=='BLOCKED' else ['NOT_RUN'];e['limitations']=['Acceptance incomplete: device, Android API wiring, runtime isolation, live network, current external protocol or dependency gate required.']
  e['tests']=sorted(set(e['tests']+previous_tests))
  if not e['limitations']:e['limitations']=['Scope-specific evidence only; full-scope qualification is incomplete.']
  e['tests']=sorted(set(e['tests']+CRITERION_LINKS.get(id,[])))
 d['updatedAt']='2026-10-06';d['coveragePolicy']='No overlap counted as additional unique features; VERIFIED_LOCAL does not qualify device/external requirements.';p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
print('Updated conservative per-entry implementation coverage.')
