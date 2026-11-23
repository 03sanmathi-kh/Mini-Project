import axios from 'axios';
export const api=axios.create({baseURL:'/api',withCredentials:true});
let csrf;
export async function initCsrf(){csrf=(await api.get('/auth/csrf')).data.token;}
api.interceptors.request.use(async c=>{if(!['get','head','options'].includes(c.method)){let cookie=document.cookie.split('; ').find(v=>v.startsWith('XSRF-TOKEN='));if(!cookie){await initCsrf();cookie=document.cookie.split('; ').find(v=>v.startsWith('XSRF-TOKEN='));}const token=cookie?decodeURIComponent(cookie.slice('XSRF-TOKEN='.length)):csrf;if(token)c.headers['X-XSRF-TOKEN']=token;}return c;});
api.interceptors.response.use(r=>r,e=>{const url=String(e.config?.url||'');const publicSupport=url.startsWith('/support')||url.includes('/api/support')||url.startsWith('/registrations');if(e.response?.status===403&&!publicSupport)console.info('[auth] Access denied (403); current session retained.');if(e.response?.status===401&&!e.config?.suppressAuthEvent&&!url.endsWith('/login')&&!publicSupport){console.info('[auth] Protected request rejected (401); clearing local session.');window.dispatchEvent(new Event('session-expired'));}return Promise.reject(e);});
export const message=e=>e.response?.status===403?'Access denied. Your current session is still active.':e.response?.data?.message||'Unable to connect. Check that the backend is running.';
