import { createContext, useContext, useState, useEffect } from "react";
import api from "../api/axiosInstance"; // axios 인스턴스 (withCredentials 설정된 버전)

const AuthContext = createContext();

/**
 * AuthProvider
 * - 앱 전역에서 로그인 상태를 관리
 * - JWT 쿠키 기반 인증 구조에 맞게 서버와 통신
 */
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null); // 로그인한 회원 정보
  const [loading, setLoading] = useState(true); // 초기 로딩 상태
  const [loggedOut, setLoggedOut] = useState(false);

  /** 로그인 성공 시 상태 갱신 */
  const login = (userData) => {
    console.log("login() 호출됨:", userData);
    setUser(userData);
  };

  // 회원정보 자동 요청
  useEffect(() => {
    if (loggedOut) return; // 로그아웃 후엔 자동으로 /info 요청 막기

    const fetchUser = async () => {
      try {
        const res = await api.get("/members/info");
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
  }, [loggedOut]); // 🔧 loggedOut 바뀔 때만 재요청

  // 로그아웃 함수 수정
  const logout = async () => {
    setLoggedOut(true); // 🔧 먼저 true로 설정해서 fetchUser 중단
    try {
      await api.post("/members/logout");
    } catch (e) {
      console.error("로그아웃 요청 실패:", e);
    } finally {
      setUser(null); // 🔧 즉시 헤더 UI에서 사용자 정보 제거
      window.location.href = "/"; // 새로고침으로 쿠키·세션 싹 정리
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

/**
 * useAuth 훅
 * - 어디서든 AuthContext 접근 가능
 */
export const useAuth = () => useContext(AuthContext);
