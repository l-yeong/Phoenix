import { createContext, useContext, useState, useEffect } from "react";
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

  /** 로그인 성공 시 상태 갱신 */
  const login = (userData) => {
    console.log("login() 호출됨:", userData);
    setUser(userData);
  };

  /** 세션 기반 회원 정보 자동 요청 */
  useEffect(() => {
    if (loggedOut) return; // 로그아웃 중에는 요청하지 않음

    const fetchUser = async () => {
      try {
        const res = await api.get("/members/info"); // 세션 쿠키 포함됨
        if (res.data.success) {
          setUser(res.data.data);
        } else {
          setUser(null);
        }
      } catch (error) {
        console.log("회원정보 불러오기 실패:", error);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    fetchUser();
  }, [loggedOut]);

  /** 로그아웃 처리 */
  const logout = async () => {
    setLoggedOut(true);
    try {
      await api.post("/members/logout"); // 세션 무효화 요청
    } catch (e) {
      console.error("로그아웃 요청 실패:", e);
    } finally {
      setUser(null);
      window.location.href = "/"; // 세션 쿠키 초기화 & 새로고침
    }
  };

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