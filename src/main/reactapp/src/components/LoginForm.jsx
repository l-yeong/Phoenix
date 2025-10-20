import { Button, TextField, Typography } from "@mui/material";
import { useState } from "react";
import styles from "../styles/Auth.module.css";
import api from "../api/axiosInstance";
import SocialLogin from "./SocialLogin";
import { useAuth } from "../api/loginstate.jsx";
import { useNavigate } from "react-router-dom";

/**
 * 일반 로그인 폼
 * - 쿠키 기반 JWT 인증에 맞춰 수정됨
 * - 백엔드에서 JWT 쿠키를 내려주면 브라우저가 자동으로 저장
 * - 프론트는 토큰을 직접 다루지 않음
 */
const LoginForm = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [mid, setMid] = useState("");
  const [password, setPassword] = useState("");

  /**
   * 로그인 요청 처리
   * - JWT 쿠키는 백엔드가 자동 발급
   * - 성공 시 AuthContext에 회원정보(mid, mno)만 저장
   */
  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const response = await api.post("/members/login", {
        mid,
        password_hash: password,
      });

      console.log("백엔드 응답:", response.data);

      const resData = response.data.data;
      if (!resData) {
        alert("서버 응답이 올바르지 않습니다.");
        return;
      }

      const memberId = resData.mid;
      const mno = resData.mno;

      console.log("로그인 성공 → mid:", memberId, "mno:", mno);

      // 쿠키는 백엔드가 내려주므로 토큰 저장 불필요
      login({ mid: memberId, mno });

      alert(`${memberId}님 환영합니다!`);
      navigate("/");
    } catch (error) {
      console.error("로그인 요청 중 오류:", error);

      if (error.response) {
        console.log("서버 응답 코드:", error.response.status);
        console.log("서버 응답 데이터:", error.response.data);
      } else if (error.request) {
        console.log("요청은 보냈지만 응답이 없습니다:", error.request);
      } else {
        console.log("요청 설정 중 오류:", error.message);
      }

      alert("로그인 실패");
    }
  };

  return (
    <div className={styles.container}>
      <form className={styles.form} onSubmit={handleLogin}>
        <Typography variant="h5" className={styles.title}>
          🔥 Phoenix 로그인
        </Typography>

        <TextField
          label="아이디"
          fullWidth
          className={styles.input}
          value={mid}
          onChange={(e) => setMid(e.target.value)}
        />

        <TextField
          label="비밀번호"
          type="password"
          fullWidth
          className={styles.input}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <Button
          variant="contained"
          className={styles.Button}
          type="submit" // e.preventDefault()는 form onSubmit에서 처리됨
        >
          로그인
        </Button>

        <SocialLogin />
      </form>
    </div>
  );
};

export default LoginForm;
