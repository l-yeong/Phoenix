// src/api/loginstate.jsx
import { createContext, useContext, useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInstance"; // withCredentials:true 설정된 axios

/**
 * 컴포넌트 역할 정의
 * React 전역에서 로그인 상태를 관리하고, 서버의 세션과 동기화.
 * 로그인·로그아웃·세션 복구·세션 만료 감지를 통합적으로 제어하는 전역 인증 관리자.
 */

/**
 * 1. 동작 흐름 
 * (1) 예를 들어 LoginForm.jsx 컴포넌트에서 api.post("/members/login") 요청
 * (2) 서버: Spring Security 세션 생성되면서 JSESSIONID 쿠키 발급 한번에 일어남
 * (3) 클라이언트 저장 하지않고 대신 세션 쿠키가 자동 저장됨
 * (4) AuthProvider.login(userData) 호출해서
 *  -> setUser(userData) -> React 전역 상태에 로그인 정보 저장
 * (6) 지금 로그인된 회원 정보를 서버에 다시 불러오는 함수인 fetchUser() 써서
 *  -> 서버에 /members/info 요청 -> SecurityContextHolder 정보 확인
 * (7) 세션 유효하면 setUser(member)로 user 상태에 최종 회원 정보 저장
 * (8) 전역 Context(AuthContext)에 user 값이 공급됨
 * (9) App 전체 컴포넌트 (Header, Mypage 등)
 * ---- useAuth()로 user, login, logout, loading 접근 가능
 * 
 * 2. 요약 :
 * 로그인 이후, 새로고침하거나 다른 페이지로 이동할 때마다
 * AuthProvider가 fetchUser()를 실행해서 서버 세션을 확인하고,
 * 여전히 로그인된 회원인지 체크함.
 */

// 전역 저장소의 역할을 하는 컨텍스트 객체 생성.
const AuthContext = createContext();

/**
 * - 세션 기반(Spring Security) 인증 구조
 * - 서버의 SecurityContextHolder 상태와 동기화 
 * children	:부모 컴포넌트가 감싸고 있는 하위 요소들이 전달되는 props
 * AuthProvider :	하위 컴포넌트들에게 인증 관련 상태(Context)를 공급하는 Provider
 * {children} :	AuthProvider 안에 포함된 JSX를 그대로 렌더링
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null); // 로그인한 회원 정보
  const [loading, setLoading] = useState(true); // 초기 로딩 상태
  const [loggedOut, setLoggedOut] = useState(false); // 로그아웃 감지 플래그
  const navigate = useNavigate(); // SPA 네비게이션 훅 추가

  /**
   * - 로그인 성공 시 호출됨
   * - 서버에서 받은 사용자 정보를 user 상태에 저장
   * - 추가적으로 fetchUser() 호출하여 서버 세션과 프론트 상태 동기화
   */
  const login = (userData) => {
    setUser(userData); // 즉시 전역 상태 반영
    fetchUser(); // 서버 세션 상태와 재동기화 시도
  };

  /**
   * - 새로고침 또는 페이지 재진입 시 서버 세션을 확인
   * - Spring Security의 SecurityContextHolder에 로그인 정보가 있으면
   *   해당 회원 정보를 반환받아 user 상태를 복원함
   * - 세션이 유효하지 않으면 user를 null로 설정
   */
  const fetchUser = async () => {
    try {
      const res = await api.get("/members/info"); // 세션 쿠키 서버로 자동 전송
      if (res.data.success) {
        setUser(res.data.data); // 서버 응답에서 회원정보 추출하여 user 상태에 저장
      } else {
        setUser(null); // 로그인 상태 아니면 user : null 로 설정
      }
    } catch (error) {
      setUser(null);
    } finally {
      setLoading(false); // 로딩 완료
    }
  };

  /**
   *  fetchUser 자동 실행
   * - 컴포넌트(앱) 최초 렌더링 시 서버 세션을 자동 확인
   * - loggedOut이 false이면서 user가 없을 때 fetchUser 실행
   * - 즉, 새로고침 시 서버의 세션 정보를 다시 가져와 로그인 상태 복원
   */
  useEffect(() => {
    if (!loggedOut && !user) {
      fetchUser();
    }
  }, [loggedOut]);

  /**
   * - 서버 세션 무효화 요청 (Spring Security logout 처리)
   * - 클라이언트 전역 상태 초기화
   * - SPA 라우팅을 통해 홈('/')으로 이동
   */
  const logout = async () => {
    setLoggedOut(true); // 로그아웃 상태 플래그 변경
    try {
      await api.post("/members/logout"); // 서버 세션 무효화 요청
    } catch (e) {
      console.error("로그아웃 요청 실패:", e);
    } finally {
      setUser(null);  // 전역 상태 초기화
      navigate("/");  // 홈으로 이동
    }
  };

  /**
   * 세션 만료 이벤트 감지
   * - axiosInstance.js에서 401 응답 시 `window.dispatchEvent(new Event("sessionExpired"))`로 이벤트 발생
   * - 여기서 해당 이벤트를 수신하여 자동 로그아웃 처리 및 로그인 페이지로 이동
   */
  useEffect(() => {
    const handleSessionExpired = () => {
      alert("세션이 만료되었습니다. 다시 로그인해주세요.");
      logout(); // 전역 상태 초기화
      navigate("/login"); // 로그인 페이지로 이동
    };

    window.addEventListener("sessionExpired", handleSessionExpired);
    return () => window.removeEventListener("sessionExpired", handleSessionExpired);
  }, [navigate]);

  /**
   *  디버깅용 useEffect
   * - user 상태가 바뀔 때마다 콘솔에 출력
   * - 현재 로그인한 회원정보 변화를 추적 가능
   */
  useEffect(() => {
  }, [user]);

  /**
   * - 하위 컴포넌트에서 useAuth()를 통해 user, login, logout, loading 접근 가능
   */
  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

/**
 * - 전역 인증 상태(AuthContext)를 편하게 가져올 수 있는 커스텀 훅
 * - 예: const { user, login, logout } = useAuth();
 *  이 구조를 통해, AuthProvider가 감싼 모든 하위 컴포넌트들(children) 은
 *  useAuth() 훅을 통해 전역 상태(user, login, logout, loading)에 접근할 수 있게 함.
 */
export const useAuth = () => useContext(AuthContext);

/**
 * AuthProvider로 App 전체를 감싸면,
 * 모든 하위 컴포넌트(children)가 같은 로그인 상태(user)를 전역적으로 공유하고,
 * React에서 어디서든 useAuth()로 로그인 정보를 쓸 수 있음.
 * React + Spring Security 세션 기반 로그인 상태를 전역으로 유지
 */
