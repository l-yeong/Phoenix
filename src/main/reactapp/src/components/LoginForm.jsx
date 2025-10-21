import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import api from "../api/axiosInstance";
import { useAuth } from "../api/loginstate.jsx";


/**
 * LoginForm.jsx
 * Phoenix 프로젝트 - 일반 로그인 + 소셜 로그인
 * 회원가입 페이지와 동일한 중앙 정렬형 디자인
 */
const LoginForm = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  const [mid, setMid] = useState("");
  const [password, setPassword] = useState("");

  /** 일반 로그인 처리 */
  const handleLogin = async (e) => {
    e.preventDefault();

    try {
      const response = await api.post("/members/login", 
        { mid, password_hash: password,} ,
        { withCredentials : true }
    );

      const resData = response.data.data;
      if (!resData) {
        alert("서버 응답이 올바르지 않습니다.");
        return;
      }

      login({ mid: resData.mid, mno: resData.mno });
      alert(`${resData.mid}님 환영합니다!`);
      navigate("/");
    } catch (error) {
    console.error("로그인 실패:", error);

    // ✅ 응답 상태별 처리 (302는 더 이상 안뜰 예정, 대신 401/400)
    if (error.response) {
      const { status } = error.response;
      if (status === 401) {
        alert("인증되지 않은 요청입니다. 다시 로그인해주세요.");
      } else if (status === 400) {
        alert("아이디 또는 비밀번호를 확인해주세요.");
      } else {
        alert("서버 오류가 발생했습니다.");
      }
    } else {
      alert("서버에 연결할 수 없습니다.");
    }
  }
  };

  /** 소셜 로그인 리디렉션 */
  const handleSocialLogin = (provider) => {
    window.location.href = `http://localhost:8080/oauth2/authorization/${provider}`;
  };

  return (
    <div
      style={{
        textAlign: "center",
        marginTop: "100px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
      }}
    >
      {/* 🔥 제목 */}
      <Typography
        variant="h5"
        sx={{ mb: 3, color: "#CA2E26", fontWeight: "bold" }}
      >
        🔥 Phoenix 로그인
      </Typography>

      {/* 🧩 로그인 폼 */}
      <Box
        component="form"
        onSubmit={handleLogin}
        sx={{
          display: "flex",
          flexDirection: "column",
          gap: 2,
          width: "100%",
          maxWidth: "400px",
        }}
      >
        <TextField
          label="아이디"
          fullWidth
          value={mid}
          onChange={(e) => setMid(e.target.value)}
        />

        <TextField
          label="비밀번호"
          type="password"
          fullWidth
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />

        <Button
          variant="contained"
          type="submit"
          sx={{
            mt: 1,
            bgcolor: "#CA2E26",
            color: "white",
            fontWeight: "bold",
            "&:hover": { bgcolor: "#b22720" },
          }}
        >
          로그인
        </Button>
      </Box>

      {/* 🔹 안내문 */}
      <Typography
        variant="body2"
        sx={{
          mt: 4,
          mb: 1,
          color: "gray",
          fontSize: "0.9rem",
        }}
      >
        SNS 계정으로 빠르게 로그인하세요
      </Typography>

      {/* 🔹 소셜 로그인 버튼 */}
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          gap: 3,
          mt: 1,
        }}
      >
        {/* Google */}
        <Button
          onClick={() => handleSocialLogin("google")}
          sx={{
            minWidth: "50px",
            height: "50px",
            borderRadius: "50%",
            bgcolor: "white",
            boxShadow: 1,
            "&:hover": { boxShadow: 3 },
          }}
        >
          <img src="/구글로고.jpg" alt="Google Login" width="24" />
        </Button>

        {/* GitHub */}
        <Button
          onClick={() => handleSocialLogin("github")}
          sx={{
            minWidth: "50px",
            height: "50px",
            borderRadius: "50%",
            bgcolor: "black",
            "&:hover": { bgcolor: "#333" },
          }}
        >
          <img src="/깃로고.jpg" alt="GitHub Login" width="24" />
        </Button>

        {/* Facebook */}
        <Button
          onClick={() => handleSocialLogin("facebook")}
          sx={{
            minWidth: "50px",
            height: "50px",
            borderRadius: "50%",
            bgcolor: "#1877f2",
            "&:hover": { bgcolor: "#155dc0" },
          }}
        >
          <img src="/페북로고.png" alt="Facebook Login" width="24" />
        </Button>
      </Box>
    </div>
  );
};

export default LoginForm;
