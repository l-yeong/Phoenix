// src/api/axiosInstance.js
import axios from "axios";

// 공통 Axios 인스턴스 생성 (쿠키 자동 전송)
const api = axios.create({
    baseURL: "http://localhost:8080",
    headers: { "Content-Type": "application/json" },
    withCredentials: true, // 쿠키 자동 포함 (핵심)
});

// [요청 인터셉터]
// 이제는 Authorization 헤더 추가 안 함
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            setUser(null);
        }
        return Promise.reject(error);
    }
);

// [응답 인터셉터]
// 인증 만료 시 처리 (쿠키 기반이라 토큰 삭제는 불필요)
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const { status, config } = error.response || {};
        if (status === 401 && config?.url !== "/members/info") {
            console.warn("인증 만료 또는 로그인 필요");
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);

export default api;
