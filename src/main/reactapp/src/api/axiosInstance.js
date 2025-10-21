// src/api/axiosInstance.js
import axios from "axios";

// 공통 Axios 인스턴스 생성 (세션 쿠키 자동 전송)
const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // JSESSIONID 쿠키 자동 포함
});

// 요청 인터셉터 (이제 Authorization 헤더 추가 안 함)
api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
);

// 응답 인터셉터
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error;

    // 세션 만료 시 → 로그인 페이지로 이동
    if (response?.status === 401) {
      console.warn("세션 만료 또는 로그인 필요");
      window.location.href = "/login";
    }

    return Promise.reject(error);
  }
);

export default api;
