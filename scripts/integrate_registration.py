from pathlib import Path
root=Path(__file__).resolve().parents[1]
def change(name,old,new):
 p=root/name;s=p.read_text(encoding='utf-8');assert old in s,(name,old[:80]);p.write_text(s.replace(old,new),encoding='utf-8')
change('frontend/src/main.jsx',"import SupportModal from './Support';","import SupportModal from './Support';\nimport RegistrationModal,{AdminRegistrationRequests} from './Registration';")
change('frontend/src/main.jsx','const pageTitles={','const pageTitles={registrations:\'Registration Requests\',')
change('frontend/src/main.jsx',"{tab==='notifications'?","{tab==='registrations'?<AdminRegistrationRequests/>:tab==='notifications'?")
change('frontend/src/main.jsx',"{user.role==='ADMIN'&&<><Nav active={tab==='users'}","{user.role==='ADMIN'&&<><Nav active={tab==='registrations'} icon=\"users\" onClick={()=>navigate('registrations')}>Registration Requests</Nav><Nav active={tab==='users'}")
change('frontend/src/main.jsx',"[support,setSupport]=useState(false);async function submit", "[support,setSupport]=useState(false),[registration,setRegistration]=useState(false),[registrationNotice,setRegistrationNotice]=useState('');async function submit")
change('frontend/src/main.jsx','<Alert type="success">{notice}</Alert><Alert>{error}</Alert>','<Alert type="success">{notice}</Alert><Alert type="success">{registrationNotice}</Alert><Alert>{error}</Alert>')
change('frontend/src/main.jsx','<button className="btn btn-secondary" onClick={()=>setSupport(true)}>Get help</button>','<button className="btn btn-secondary register-entry" onClick={()=>setRegistration(true)}>Register</button>')
change('frontend/src/main.jsx','{support&&<SupportModal onClose={()=>setSupport(false)}/>}','{support&&<SupportModal onClose={()=>setSupport(false)}/>} {registration&&<RegistrationModal onClose={()=>setRegistration(false)} onHelp={()=>{setRegistration(false);setSupport(true)}} onActivated={loginEmail=>{setRegistration(false);setEmail(loginEmail);setPassword(\'\');setRegistrationNotice(\'Your account is active. Sign in with your PermitWorks login email.\')}}/>}')
change('frontend/src/api.js',"const publicSupport=url.startsWith('/support')||url.includes('/api/support');", "const publicSupport=url.startsWith('/support')||url.includes('/api/support')||url.startsWith('/registrations');")
change('frontend/src/api.js',"if(e.response?.status===403)console.info", "if(e.response?.status===403&&!publicSupport)console.info")
change('frontend/vite.config.js',"'/api':'http://localhost:8080'","'/api':process.env.API_PROXY_TARGET||'http://localhost:8080'")
print('Existing login and Admin navigation integrated; support preserved.')
