import axios from 'axios';

const api = axios.create({
    baseURL : "http://localhost:8080",
    headers : { "Contest-Type" : "application/json" }
});

// [요청 인터셉터] Access Token을 헤더에 포함시키기
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("accessToken");
    if(token) config.headers.Authorization = `Bearer${token}`;
    return config;
});

// [응답 인터셉터] 에러처리 or 토큰 재발급
api.interceptors.response.use(
    (response) => reponse,
    async(error) => {
        const {status} = error.response || {};
        if(status === 401){
            console.warn("인증 만료: 다시 로그인 필요");
            localStorage.removeItem("accessToken");
            window.location.href = "/login";
        }
        return Promise.reject(error);
    }
);
export default api;