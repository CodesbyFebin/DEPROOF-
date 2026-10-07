#!/usr/bin/env python3
"""Separate absent code, partial implementation and unexecuted acceptance without guessing completion."""
import collections,json,pathlib
r=pathlib.Path(__file__).resolve().parents[1];out=r/'evidence/qualification'
u=json.loads((out/'uncovered-requirements.json').read_text());status=json.loads((out/'implementation-status.json').read_text())
lookup={e['id']:e for values in status['coverage'].values() for e in values}
local_missing={'F055':('P2','bounded private preview and MIME refusal'),'F058':('P2','explicit optional location consent and collection'),'F093':('P1','supported account-creation/rent explanations'),'F105':('P2','mapping task planner'),'F115':('P2','persisted reminder scheduling/preferences'),'F117':('P4','complete translation coverage'),'FN061':('P1','optional consented explanation contract/provider staging')}
external={'F081','F082','F083','F084','F085','F086','F087','F088','F089','F090','F101','F102','F103','F106','F107','F110','F111','F112','F113','E008','E009','E010','E015','FN061'}
security=set('F040 F041 F045 F046 F047 F048 F049 F060 F067 F079 F097 F118 FN033 FN048 FN051 FN052 FN055 FN062 C045 C046 C047 C048 C049 C095 C097'.split())
rows=[]
for x in u['requirements']:
 id=x['id'];e=lookup[id];phase=e.get('phase','P3')
 kind='MISSING_IMPLEMENTATION' if x['status']=='NOT_STARTED' else 'PARTIAL_IMPLEMENTATION_OR_VERIFICATION'
 availability='LOCAL_CODE_AND_TESTS';dependencies=[]
 if id in external:availability='EXTERNAL_PROTOCOL_OR_PROVIDER';dependencies=['current authoritative protocol and authorized provider access']
 elif id in security or id.startswith('C'):availability='LOCAL_TESTS_PLUS_DEVICE_ACCEPTANCE';dependencies=['supporting local tests','authorized physical device / compatible wallet where applicable']
 if id in local_missing:phase,work=local_missing[id];dependencies.append(work)
 if id=='FN033':kind='IMPLEMENTED_MISSING_DEVICE_VERIFICATION';availability='PHYSICAL_KEYSTORE';dependencies=['physical hardware-backed Keystore signing and observed KeyInfo']
 if id=='FN061':kind='MISSING_EXTERNAL_ADAPTER';dependencies=['qualified consented provider adapter; existing local guard is not an integration']
 if id=='F093':kind='PARTIAL_IMPLEMENTATION_OR_VERIFICATION';dependencies=['broader account-creation layouts and actual rent observation if expanded','Android rendering acceptance']
 if id in ['F055','F095']:kind='IMPLEMENTED_MISSING_ANDROID_VERIFICATION';availability='LOCAL_TESTS_PLUS_DEVICE_ACCEPTANCE';dependencies=['private fixture preview / persisted first-seen Android lifecycle observations']
 if id in ['F058','F105','F115']:kind='IMPLEMENTED_MISSING_ANDROID_VERIFICATION';availability='LOCAL_TESTS_PLUS_DEVICE_ACCEPTANCE';dependencies=['physical permission/lifecycle/reopen/export or actual notification observations; see docs/local-workflows.md']
 if id=='F117':kind='PARTIAL_IMPLEMENTATION_OR_VERIFICATION';dependencies=['dynamic decoder/account-effect and remaining fallback localization','independent Spanish review; device locale/accessibility/restart checks']
 if id=='F097':kind='IMPLEMENTED_MISSING_DEVICE_VERIFICATION';dependencies=['expired devnet blockhash handoff and reapproval observation']
 rank=0 if id in security and availability not in ['PHYSICAL_KEYSTORE','EXTERNAL_PROTOCOL_OR_PROVIDER'] else 1 if id in local_missing and id not in ['F055','F093','FN061'] else 2 if availability=='LOCAL_CODE_AND_TESTS' else 3
 rows.append({'id':id,'registryStatus':x['status'],'gapKind':kind,'priority':rank,'phase':phase,'environment':availability,'dependencies':dependencies,'acceptance':x['acceptance'],'implementationPaths':x['implementedPaths'],'tests':x['namedTests'],'evidencePaths':e.get('evidencePaths',[]),'remainingReasons':x['requiredUncoveredGate']})
rows.sort(key=lambda x:(x['priority'],x['phase'],x['id']))
result={'schema':'deproof-prioritized-backlog-v1','policy':'Priority 0 security prerequisites; 1 absent local code; 2 partial local code/review; 3 hardware/external evidence. Nonempty paths never establish complete implementation. Mixed gaps require code and evidence review.','unresolvedCount':len(rows),'gapCounts':dict(collections.Counter(x['gapKind'] for x in rows)),'requirements':rows}
(out/'prioritized-backlog.json').write_text(json.dumps(result,indent=2)+'\n')
print('PASS: prioritized '+str(len(rows))+' unresolved entries; no completion inferred from registry existence.')
