const fs=require('fs');const path=require('path');const assert=require('assert');
const {chromium}=require('C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1119-907-0_26.1119.907.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/playwright');
const root=path.resolve(__dirname,'..');const xml=fs.readFileSync(path.join(root,'backend/.idea/workspace.xml'),'utf8');
const password=process.env.ADMIN_PASSWORD||xml.match(/name="ADMIN_PASSWORD"\s+value="([^"]+)"/)?.[1]?.replaceAll('&amp;','&').replaceAll('&quot;','"');
if(!password)throw new Error('No trusted local Admin credential configured');
(async()=>{const browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});const page=await browser.newPage({viewport:{width:1440,height:1080}});const results=[];const errors=[];page.on('pageerror',e=>errors.push(e.message));
try{
 await page.goto(process.env.PERMIT_UI_URL||'http://localhost:5174',{waitUntil:'networkidle',timeout:60000});await page.getByLabel('PermitWorks email').waitFor();await page.screenshot({path:path.join(root,'review_evidence/screenshots/login.png')});
 await page.getByLabel('PermitWorks email').fill('admin@permitworks.com');await page.locator('#password').fill(password);await page.getByRole('button',{name:'Sign in',exact:true}).click();
 await page.getByRole('button',{name:'Assignment and workload',exact:true}).waitFor({timeout:30000});results.push({check:'Admin sign in through React and live API',status:'PASS'});
 // Privacy redaction affects only contact identity and officer names in evidence images.
 await page.addStyleTag({content:'.sidebar-account{visibility:hidden!important}.table-wrap tbody td:first-child{filter:blur(7px)!important}'});
 for(const [name,heading,file] of [['Assignment and workload','Assignment and workload','workload.png'],['Reference analytics','Reference analytics','reference-analytics.png'],['Audit history','Audit history','audit.png'],['Notifications','Notifications','notifications.png']]){
  await page.getByRole('button',{name,exact:true}).click();await page.getByRole('heading',{name:heading,exact:true}).waitFor();
  await page.waitForLoadState('networkidle');assert(!await page.getByText('Unable to connect. Check that the backend is running.',{exact:true}).count());
  await page.screenshot({path:path.join(root,'review_evidence/screenshots',file),fullPage:true});results.push({check:name+' renders from live backend',status:'PASS'});
  if(name==='Assignment and workload')await page.locator('.panel').filter({has:page.getByRole('heading',{name:'Officer workload',exact:true})}).screenshot({path:path.join(root,'review_evidence/screenshots/workload-report.png')});
 }
 assert.equal(errors.length,0,'Browser page errors: '+errors.join('; '));results.push({check:'No browser page errors',status:'PASS'});
 await page.addStyleTag({content:'.sidebar-account{visibility:visible!important}'});await page.getByRole('button',{name:'Sign out',exact:true}).click();await page.getByRole('button',{name:'Sign in',exact:true}).waitFor();
 fs.writeFileSync(path.join(root,'review_evidence/integration/browser-smoke.json'),JSON.stringify({results,screenshotPrivacy:'Contact identity hidden and officer-name cells blurred; no workflow output replaced.'},null,2));console.log(JSON.stringify(results));
}finally{await browser.close()}})().catch(e=>{console.error(e.message);process.exitCode=1});
