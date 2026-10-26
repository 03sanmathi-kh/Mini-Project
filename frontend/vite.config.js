import {defineConfig} from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig({plugins:[react()],server:{allowedHosts:["terminal.local"],port:5173,strictPort:true,proxy:{'/api':'http://localhost:8080','/swagger-ui':'http://localhost:8080','/v3':'http://localhost:8080'}},preview:{port:5173}});
