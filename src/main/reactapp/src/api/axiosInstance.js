// src/api/axiosInstance.js
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

api.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const { response } = error;

    if (response?.status === 401) {
      console.warn("세션 만료 또는 로그인 필요");

      // 직접 새로고침 대신 전역 이벤트 발행
      window.dispatchEvent(new Event("sessionExpired"));
    }

    return Promise.reject(error);
  }
);

export default api;
