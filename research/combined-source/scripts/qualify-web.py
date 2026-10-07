#!/usr/bin/env python3
import pathlib,subprocess
from html.parser import HTMLParser
from urllib.parse import urlsplit,unquote
r=pathlib.Path(__file__).resolve().parents[1]
class Parse(HTMLParser):
 def __init__(self):super().__init__();self.links=[];self.ids=set();self.lang=False;self.viewport=False;self.main=False
 def handle_starttag(self,tag,attrs):
  a=dict(attrs)
  if 'id' in a:self.ids.add(a['id'])
  if tag=='html':self.lang=a.get('lang')=='en'
  if tag=='meta' and a.get('name')=='viewport':self.viewport=True
  if tag=='main':self.main=True
  for k in ['href','src']:
   if k in a:self.links.append(a[k])
for p in (r/'web').glob('*.html'):
 parser=Parse();parser.feed(p.read_text());assert parser.lang and parser.viewport and parser.main,(p,'missing semantics')
 for link in parser.links:
  u=urlsplit(link)
  if u.scheme or u.netloc:continue
  if u.path:assert (p.parent/unquote(u.path)).resolve().exists(),(p.name,'broken link',link)
  elif u.fragment:assert u.fragment in parser.ids,(p.name,'broken fragment',link)
subprocess.run(['node','--check',str(r/'web/inspect.js')],check=True)
assert len(list((r/'web').glob('*.html')))==7
print('PASS: seven page semantics, repository-relative links/assets and JS syntax. Browser responsiveness/accessibility/interaction gates NOT_RUN.')
