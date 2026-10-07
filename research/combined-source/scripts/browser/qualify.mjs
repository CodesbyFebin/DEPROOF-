import { chromium } from 'playwright';
import AxeBuilder from '@axe-core/playwright';
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..');
const out=path.join(root,'evidence/qualification/browser');fs.mkdirSync(out,{recursive:true});
const mime={'.html':'text/html','.css':'text/css','.js':'text/javascript','.svg':'image/svg+xml','.png':'image/png','.json':'application/json'};
const server=http.createServer((req,res)=>{const url=new URL(req.url,'http://localhost');let target=path.resolve(root,'web','.'+decodeURIComponent(url.pathname));if(url.pathname==='/')target=path.join(root,'web/index.html');if(!target.startsWith(path.join(root,'web')+path.sep)){res.writeHead(403).end();return;}try{res.setHeader('Content-Type',mime[path.extname(target)]??'application/octet-stream');res.end(fs.readFileSync(target));}catch{res.writeHead(404).end();}});
await new Promise(r=>server.listen(0,'127.0.0.1',r));const base=`http://127.0.0.1:${server.address().port}`;
const browser=await chromium.launch({headless:true,chromiumSandbox:true});
const checks=[];const failures=[];
function check(id,ok,detail){checks.push({id,status:ok?'PASS':'FAIL',detail});if(!ok)failures.push(id);console.log(`${ok?'PASS':'FAIL'}: ${id}`);fs.writeFileSync(path.join(out,'checks-in-progress.json'),JSON.stringify(checks,null,2));}
try {
 for(const width of [320,1280]) {
  const context=await browser.newContext({viewport:{width,height:900},reducedMotion:'reduce'});
  for(const name of ['index','features','how-it-works','ecosystem','project','documentation','privacy']) {
   const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));const response=await page.goto(`${base}/${name}.html`,{waitUntil:'networkidle'});
   check(`${name}-${width}-loads`,response.status()===200 && errors.length===0,{errors});
   for(const href of await page.locator('a[href^="delivery/"]').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('href')))){const linked=await page.request.get(new URL(href,page.url()).href);check(`${name}-${width}-link-${href}`,linked.status()===200,{status:linked.status()});}
   for(const img of await page.locator('img').all())await img.scrollIntoViewIfNeeded();
   await page.waitForFunction(()=>[...document.images].every(img=>img.complete));
   check(`${name}-${width}-images`,await page.evaluate(()=>[...document.images].every(img=>img.naturalWidth>0)));
   await page.evaluate(()=>scrollTo(0,0));
   const layout=await page.evaluate(()=>({viewport:innerWidth,scroll:document.documentElement.scrollWidth,main:document.querySelector('main').getBoundingClientRect().width}));
   check(`${name}-${width}-no-document-overflow`,layout.scroll<=width+1,layout);
   const axe=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa','wcag22aa']).analyze();
   check(`${name}-${width}-axe`,axe.violations.length===0,axe.violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>n.target)})));
   await page.keyboard.press('Tab');check(`${name}-${width}-keyboard-skip-link`,await page.locator('.skip').evaluate(el=>el===document.activeElement));
   await page.keyboard.press('Enter');check(`${name}-${width}-skip-target`,await page.evaluate(()=>location.hash==='#main'));
   await page.screenshot({path:path.join(out,`${name}-${width}.png`),fullPage:true});
   if(width===320){await page.addStyleTag({content:':root{font-size:34px}'});const zoom=await page.evaluate(()=>({ok:document.documentElement.scrollWidth<=innerWidth+1,overflow:[...document.querySelectorAll('body *')].filter(el=>el.getBoundingClientRect().right>innerWidth+1 && !el.closest('.table-scroll')).map(el=>({tag:el.tagName,cls:el.className,text:el.textContent.slice(0,60),width:el.getBoundingClientRect().width})).slice(0,12)}));check(`${name}-200-percent-text`,zoom.ok,zoom.overflow);}
   await page.close();
  }
  await context.close();
 }
 const page=await browser.newPage();await page.goto(`${base}/features.html`);await page.locator('#filter').fill('F001');check('feature-filter-single-match',await page.locator('tr[data-search]:visible').count()===1);await page.locator('#filter').fill('no-such-feature-xyz');check('feature-filter-empty-state',await page.locator('#filter-count').textContent()==='0 of 120 base features shown');
 await page.goto(`${base}/how-it-works.html`);await page.locator('#receipt-text').fill('{broken');await page.locator('#inspect').click();check('receipt-invalid-json-error',(await page.locator('#inspection').textContent()).startsWith('Cannot inspect:'));
 const receipt={schema:'deproof-receipt-v2',outcome:'REJECTED',signature:null,submission:{broadcast:false,submittedByDeproof:false},chainObservation:{availability:'NOT_QUERIED',lastKnownStatus:'UNKNOWN'},network:'devnet',account:null,messageSha256:null,evidenceDigest:null,limitations:['Synthetic browser test fixture; not a transaction']};await page.locator('#receipt-text').fill(JSON.stringify(receipt));await page.locator('#inspect').click();check('receipt-inspection-does-not-claim-crypto-verification',(await page.locator('#inspection').textContent()).includes('cryptographic verification requires'));
 receipt.signature='invented';await page.locator('#receipt-text').fill(JSON.stringify(receipt));await page.locator('#inspect').click();check('receipt-provenance-conflict-rejected',(await page.locator('#inspection').textContent()).includes('REJECTION_PROVENANCE_CONFLICT'));
 fs.writeFileSync(path.join(out,'report.json'),JSON.stringify({schema:'deproof-browser-qualification-v1',browserVersion:browser.version(),status:failures.length?'FAIL':'PASS',checks,limitations:['Chromium desktop engine at tested viewports only.','Automated axe rules and keyboard checks do not establish comprehensive screen-reader or cognitive accessibility qualification.','Physical Android Compose accessibility remains NOT_RUN.']},null,2));
} finally {await browser.close();server.close();}
if(failures.length)process.exitCode=1;
