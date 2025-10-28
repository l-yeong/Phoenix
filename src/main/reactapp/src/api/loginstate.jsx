// src/api/loginstate.jsx
import { createContext, useContext, useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInstance"; // withCredentials:true 설정된 axios

const AuthContext = createContext();

/**
 * AuthProvider
 * - 세션 기반(Spring Security) 인증 구조
 * - 서버의 SecurityContextHolder 상태와 동기화
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null); // 로그인한 회원 정보
  const [loading, setLoading] = useState(true); // 초기 로딩 상태
  const [loggedOut, setLoggedOut] = useState(false); // 로그아웃 감지 플래그
  const navigate = useNavigate(); // SPA 네비게이션 훅 추가

  /** 로그인 성공 시 상태 갱신 */
  const login = (userData) => {
    console.log("login() 호출됨:", userData);
    setUser(userData);
    // fetchUser();
  };

  // const fetchUser = async () => {
  //     console.log("fetchUser 체크");
  //     try {
  //       const res = await api.get("/members/info"); // 세션 쿠키 포함됨
  //       console.log("서버응답",res);
  //       if (res.data.success) {
  //         console.log("회원정보",res.data.data);
  //         setUser(res.data.data.member);
  //       } else {
  //         console.log("[fetchUser] 로그인 상태 아님");
  //         setUser(null);
  //       }
  //     } catch (error) {
  //       console.log("회원정보 불러오기 실패:", error);
  //       setUser(null);
  //     } finally {
  //       console.log("[fetchUser] 완료");
  //       setLoading(false);
  //     }
  //   };


  /** 세션 기반 회원 정보 자동 요청 */
  // useEffect(() => {
  //   if (!loggedOut && !user) {
  //     fetchUser();
  //   }
  // }, []);
  /** 로그아웃 처리 */
  const logout = async () => {
    setLoggedOut(true);
    try {
      await api.post("/members/logout"); // 세션 무효화 요청
    } catch (e) {
      console.error("로그아웃 요청 실패:", e);
    } finally {
      setUser(null);
      // 새로고침 대신 SPA 라우팅
      navigate("/");
    }
  };

  /** 세션 만료 이벤트 감지 (axiosInstance.js에서 dispatch된 이벤트 잡기) */
  useEffect(() => {
    const handleSessionExpired = () => {
      console.warn("세션 만료 이벤트 수신됨 → 로그인 페이지 이동");
      alert("세션이 만료되었습니다. 다시 로그인해주세요.");
      logout(); // 전역 상태 초기화
      navigate("/login"); // SPA 내부 이동
    };

    window.addEventListener("sessionExpired", handleSessionExpired);
    return () => window.removeEventListener("sessionExpired", handleSessionExpired);
  }, [navigate]);

  // 디버깅용 로그
  useEffect(() => {
    console.log("AuthContext user 변경:", user);
  }, [user]);

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};

/** useAuth 훅 (전역 상태 접근용) */
export const useAuth = () => useContext(AuthContext);
