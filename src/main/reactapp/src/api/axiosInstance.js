// src/api/axiosInstance.js
import axios from "axios";

/**
 * axiosInstance.js 역할 정의
 * 모든 통신의 중간 관리자로,
 * 요청/응답/에러를 한 곳에서 감시하려고 만듬
 * 세션 만료 같은 전역 상황은 이벤트로 앱 전체에 알려서
 * 나머지 일반 에러는 Promise를 통해 각 컴포넌트로 되돌려보내는 구조.
 */

/**
 * 전체 동작 흐름
 * (1) 클라이언트 -> 서버 요청 (api.get/post)
 * (2) 만약 로그인하는데 서버에서 401 응답 (세션 만료 응답 보내면)
 * (3) axios.response 인터셉터가 감지
 * (4) 브라우저 전체에서 dispatchEvent("sessionExpired") 발생시킴
 * (5) AuthProvider에서 addEventListener로 감지
 * (6) handleSessionExpired() -> logout() -> navigate("/login")
 */

/**
 * axios 인스턴스 생성
 * - 기본 설정(baseURL, 헤더, 쿠키 전송 옵션 등)을 모든 요청에 공통 적용
 * - 이후 import해서 api.get(), api.post() 형태로 사용 가능
 */
const api = axios.create({
  baseURL: "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // 세션 쿠키를 자동으로 포함시킴
});

/**
 * 요청 인터셉터 (Request Interceptor)
 * - axios가 요청을 서버로 보내기 전에 호출됨
 * - 공통적으로 헤더 추가, 토큰 삽입, 로깅 등의 처리를 할 수 있음
 */

/* ==============================
   요청 인터셉터 (Request)
============================== */
api.interceptors.request.use(
  (config) => config,
  (error) => {
    // 콘솔 출력 제거 (필요하면 아래 한 줄만 유지)
    // console.error("[Axios] 요청 에러:", error);
    return Promise.reject(error);
  }
);


/**
 * 응답 인터셉터 (Response Interceptor)
 * - 서버에서 응답을 받은 뒤 호출됨
 * - 성공 응답은 그대로 반환, 에러 응답은 별도 처리 가능
 */
api.interceptors.response.use(
  (response) => {
    return response;
    // 정상 응답은 그대로 반환
  },
  (error) => {
    const { response } = error;
    // 서버에서 오류 응답을 받았거나, 네트워크 에러가 발생한 경우

    /**
     *  401 Unauthorized 감지
     * - 세션 만료 / 인증 실패 시 서버가 401 응답을 보냄
     * - 여기서 커스텀 이벤트(sessionExpired)를 전역으로 발생시켜
     *   AuthProvider 등에서 감지 후 자동 로그아웃 및 로그인 페이지 이동 처리
     */
    if (response?.status === 401) {

      window.dispatchEvent(new Event("sessionExpired"));
      //  window.dispatchEvent()
      // - 브라우저 전역(window)에서 커스텀 이벤트를 발생시킴
      // - AuthProvider의 useEffect가 addEventListener로 이 이벤트를 감지함
      // - 감지되면 handleSessionExpired() 실행 -> logout() + navigate("/login")
    }

    return Promise.reject(error);
    // Promise.reject(error)
    // - 이 응답 인터셉터에서 에러를 소비하지 않고
    // - axios를 호출한 컴포넌트 로 다시 전달
    // - 이렇게 해야 각 페이지에서 개별적으로 에러 처리 가능
  }
);

export default api;