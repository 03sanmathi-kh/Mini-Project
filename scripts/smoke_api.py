from pathlib import Path
import http.cookiejar,json,urllib.request,urllib.error
from local_review import config
ROOT=Path(__file__).resolve().parents[1];env=config();jar=http.cookiejar.CookieJar();client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar));base='http://localhost:8080/api';results=[]
def request(path,payload=None,token=None):
 headers={}
 if payload is not None:headers['Content-Type']='application/json'
 if token:headers['X-XSRF-TOKEN']=token
 req=urllib.request.Request(base+path,data=None if payload is None else json.dumps(payload).encode(),headers=headers)
 with client.open(req,timeout=30) as response:return response.status,json.load(response)
_,csrf=request('/auth/csrf');token=csrf['token'];email=env.get('ADMIN_LOCAL_PART','admin')+'@permitworks.com'
status,user=request('/auth/login',{'email':email,'password':env['ADMIN_PASSWORD']},token);assert status==200 and user['role']=='ADMIN';results.append({'check':'Admin login with existing trusted configuration','status':status})
for path in ['/auth/me','/permits/dashboard','/reports/workload','/routing/profiles','/notifications','/audit','/reference/analytics']:
 status,data=request(path);assert status==200;results.append({'endpoint':path,'status':status,'shape':'array' if isinstance(data,list) else 'object','count':len(data) if isinstance(data,list) else None})
_,csrf=request('/auth/csrf');token=csrf['token']
status,_=request('/auth/activity',{},token);assert status==200;results.append({'endpoint':'POST /auth/activity','status':status})
_,csrf=request('/auth/csrf');token=csrf['token']
old_auth_cookie=next(c for c in jar if c.name=='PERMIT_TOKEN')
status,_=request('/auth/logout',{},token);assert status==200
jar.set_cookie(old_auth_cookie)
try:request('/auth/me');raise AssertionError('Logged-out token remained active')
except urllib.error.HTTPError as error:assert error.code==401;results.append({'check':'Logout revocation','status':error.code})
path=ROOT/'review_evidence/integration/live-api-smoke.json';path.write_text(json.dumps(results,indent=2),encoding='utf-8');print(json.dumps(results,indent=2))
