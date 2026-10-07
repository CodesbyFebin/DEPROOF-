#!/usr/bin/env python3
"""Validate catalogs/format contracts; never certifies translation quality or device layout."""
import collections,json,pathlib,re,xml.etree.ElementTree as ET,datetime
r=pathlib.Path(__file__).resolve().parents[1];out=r/'evidence/qualification';out.mkdir(exist_ok=True)
checks=[]
def check(name,ok):
 checks.append({'id':name,'status':'PASS' if ok else 'FAIL'});assert ok,name

def catalog(path):
 rows=list(ET.parse(path).getroot());check(str(path.relative_to(r))+'_unique_ids',len({(x.tag,x.attrib['name']) for x in rows})==len(rows))
 return {(x.tag,x.attrib['name']):x for x in rows}
a=catalog(r/'app/src/main/res/values/strings.xml');b=catalog(r/'app/src/main/res/values-es/strings.xml')
check('complete_declared_catalog_ids',set(a)==set(b))
placeholder=lambda s:collections.Counter(re.findall(r'%(\d+)\$([sd])',s))
for key,base in a.items():
 translated=b[key]
 if base.tag=='string':
  text=''.join(base.itertext());other=''.join(translated.itertext());check(key[1]+'_nonempty_and_format',bool(other) and placeholder(text)==placeholder(other))
 else:
  forms={x.attrib['quantity']:x.text for x in base};other={x.attrib['quantity']:x.text for x in translated};check(key[1]+'_plural_forms',set(forms)==set(other)=={'one','other'})
  for form in forms:check(key[1]+'_'+form+'_format',placeholder(forms[form])==placeholder(other[form]))
refs=set()
for p in (r/'app/src/main/java').rglob('*.kt'):refs.update(re.findall(r'R\.string\.([a-z0-9_]+)',p.read_text()))
check('all_resource_references_exist',refs<=set(k[1] for k in a if k[0]=='string'))
check('brand_credit_preserved',any('Built by CodesbyFebin' in (x.text or '') for x in b.values()))
report={'schema':'deproof-localization-v1','checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'status':'PASS','supportedInterfaceCatalogs':['en','es'],'stringCount':sum(k[0]=='string' for k in a),'checks':checks,'remainingAcceptance':[{'status':'NOT_RUN','gate':'Independent Spanish linguistic/security-meaning review and physical-device screen reader, narrow/200% text layout and locale switching/restart.'},{'status':'NOT_RUN','gate':'Complete localization of domain-generated decoder/account-effect explanations and remaining provider-change/fallback copy; original signed payloads/protocol codes/user content must stay exact.'}],'completeFeatureAcceptance':False}
(out/'localization-report.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS:',report['stringCount'],'matching English/Spanish catalog strings; plural/format contracts. Full F117 acceptance remains incomplete.')
