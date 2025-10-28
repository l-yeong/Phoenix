import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'
import path from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server:{
  host:true,        //외부 기기(스마트폰)에서도 접속 허용
  port:5173,        //동일 포트 유지
  strictPort:true,  //다른포트로 자동 변경방지
  cors: true,       // 필요시 CORS 허용
  },
})
