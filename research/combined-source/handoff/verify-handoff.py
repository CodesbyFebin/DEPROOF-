#!/usr/bin/env python3
import pathlib,hashlib,sys
r=pathlib.Path(__file__).resolve().parents[1];bad=[];count=0
for line in (r/'handoff/SHA256SUMS').read_text().splitlines():
 h,n=line.split('  ',1);p=r/n;count+=1
 if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=h:bad.append(n)
print(('FAIL '+repr(bad)) if bad else f'PASS: {count} delivered files match; builds and device qualification NOT_RUN')
sys.exit(bool(bad))
