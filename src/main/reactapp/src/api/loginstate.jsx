import { createContext, useContext, useState , useEffect } from "react";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {

  // 초기 렌더 시 localStorage에서 즉시 값 읽기
  const [user, setUser] = useState(() => {
    const token = localStorage.getItem("accessToken");
    const mid = localStorage.getItem("mid");
    const mno = localStorage.getItem("mno");
    return token && mid ? { mid, mno } : null;
  });

  useEffect(() => {
    console.log("AuthContext user 변경:", user);
  }, [user]);

  // 로그인 시 상태 + 로컬스토리지 동기화
  const login = (userData) => {
    const newUser = { ...userData };
    console.log("login() 호출됨:", userData);
    setUser(userData);
    localStorage.setItem("accessToken", userData.token);
    localStorage.setItem("mid", userData.mid);
    localStorage.setItem("mno", userData.mno);
  };

  // 로그아웃 시 상태 초기화
  const logout = () => {
    setUser(null);
    localStorage.clear();
    window.location.href = "/"; // 홈으로 이동
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

// 훅으로 쉽게 가져다 쓰기
export const useAuth = () => useContext(AuthContext);
