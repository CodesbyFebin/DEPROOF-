#!/usr/bin/env python3
import json,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
expected={'features':('features',[f'F{i:03}' for i in range(1,121)]),'core-checks':('checks',[f'C{i:03}' for i in range(1,101)]),'functions':('functions',[f'FN{i:03}' for i in range(1,63)]+[f'EF{i:03}' for i in range(1,25)]),'ecosystem-requirements':('requirements',[f'E{i:03}' for i in range(1,31)])}
for name,(key,ids) in expected.items():
 d=json.loads((root/(name+'.json')).read_text());records=(d["base"]+d["ecosystem"] if name=="functions" else d[key]);assert [r['id'] for r in records]==ids,(name,'IDs/counts changed');assert d['count']==len(ids)
 original_data=json.loads((root/'docs/blueprint/DEPROOF_MASTER_BLUEPRINT_AND_DESIGN'/(name+'.json')).read_text()); source=original_data['base']+original_data['ecosystem'] if name=='functions' else original_data[key]
 for entry,original in zip(records,source):
  for field in ('definition','contract','behavior','name','acceptance'):
   if field in original:assert entry[field]==original[field],(entry['id'],'authoritative meaning changed')
  for field in ('phase','implementationPaths','tests','evidencePaths','status','limitations','dependencies','sourceMappings','qualificationDimensions','checks'):assert field in entry,(entry['id'],'missing coverage',field)
  for p in entry['implementationPaths']+entry['evidencePaths']:assert (root/p).exists(),(entry['id'],'missing path',p)
  if entry['status']=='VERIFIED':assert entry['tests'] and entry['evidencePaths'] and entry['implementationPaths']
print('PASS: exact 120 F / 100 C / 62 FN + 24 EF / 30 E and per-entry coverage; no overlap counted as unique features')
