import axios from 'axios';

// 공통 Axios 인스턴스 생성
const api = axios.create({
    baseURL: "http://localhost:8080",
    headers: { "Content-Type": "application/json" }
});

// [요청 인터셉터] 
// 모든 요청마다 LocalStorage에서 accesstoken 꺼내서 Authorization 헤더에 자동 추가
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("accessToken");
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        // 디버깅용 
        console.log(
            `%c [Request] ${config.method?.toUpperCase()} ${config.url}`,
            config.data || ""
        );

        return config;
    },
    (error) => Promise.reject(error)
);

// [응답 인터셉터] 에러처리 or 토큰 재발급
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const { status } = error.response || {};
        if (status === 401) {
            console.warn("인증 만료: 다시 로그인 필요");
            localStorage.removeItem("accessToken");
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);
export default api;