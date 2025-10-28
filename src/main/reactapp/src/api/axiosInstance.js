// src/api/axiosInstance.js
import axios from "axios";

const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

api.interceptors.request.use(
  (config) => {
    console.log("[Axios] 요청 전 config:", config);
    return config;
  },
  (error) => {
    console.error("[Axios] 요청 에러:", error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => {
    console.log("[Axios] 서버 응답:", response);
    return response;
  },
  (error) => {
    const { response } = error;
    console.error("[Axios] 서버 에러:", error);

    if (response?.status === 401) {
      console.warn("[Axios] 세션 만료 또는 로그인 필요");
      window.dispatchEvent(new Event("sessionExpired"));
    }

    return Promise.reject(error);
  }
);

export default api;
