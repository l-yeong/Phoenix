//import { defineConfig } from 'vite'
//import react from '@vitejs/plugin-react-swc'
//
//// https://vite.dev/config/
//export default defineConfig({
//  plugins: [react()],
//})
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // 🔹 백엔드 API 프록시 설정
      '/tickets': {
        target: 'http://localhost:8080', // 스프링 부트 서버 주소
        changeOrigin: true,
      },
      '/upload': {
        target: 'http://localhost:8080', // 이미지 파일 요청도 백엔드로
        changeOrigin: true,
      },
    },
  },
})
//프론트 (React, Vite)는 기본적으로 localhost:5173에서 실행됨
//백엔드 (Spring Boot)는 localhost:8080에서 실행됨
//즉, 서로 다른 포트 / 다른 도메인 — 브라우저 보안 정책(CORS) 때문에 직접 호출이 안됌
//요청이 막혀서 Vite가 대신 HTML을 돌려줌
//5173번 포트로 들어온 특정 경로를 8080번 백엔드로 대신 전달해주는 터널 역할