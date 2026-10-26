import axios from 'axios';
export const api=axios.create({baseURL:'/api',withCredentials:true});
let csrf;
export async function initCsrf(){csrf=(await api.get('/auth/csrf')).data.token;}
api.interceptors.request.use(c=>{if(!['get','head','options'].includes(c.method)&&csrf)c.headers['X-XSRF-TOKEN']=csrf;return c;});
api.interceptors.response.use(r=>r,e=>{if(e.response?.status===401&&!e.config.suppressAuthEvent&&!e.config.url.endsWith('/login'))window.dispatchEvent(new Event('session-expired'));return Promise.reject(e);});
export const message=e=>e.response?.data?.message||'Unable to connect. Check that the backend is running.';
