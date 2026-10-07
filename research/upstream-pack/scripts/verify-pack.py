#!/usr/bin/env python3
import pathlib,hashlib,json,sys
root=pathlib.Path(__file__).resolve().parents[1]
bad=[]
for line in (root/'SHA256SUMS').read_text().splitlines():
 expected,name=line.split('  ',1);p=root/name
 if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=expected:bad.append(name)
for f in json.loads((root/'catalog/file-provenance.json').read_text()):
 b=(root/f['local_path']).read_bytes()
 if hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()!=f['git_blob_sha1']:bad.append(f['local_path'])
print('FAIL: '+repr(bad) if bad else 'PASS: package hashes and upstream blob identities match')
sys.exit(bool(bad))
