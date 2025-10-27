import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  Link
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
        { mid, password_hash: password, },
        { withCredentials: true }
      );

      const resData = response.data.data;
      if (!resData) {
        alert("서버 응답이 올바르지 않습니다.");
        return;
      }

      // member + role 정보를 함께 loginstate에 저장
      login({
        mid: resData.member.mid,
        mno: resData.member.mno,
        role: resData.role,
        status: resData.member.status,
      });

      // ROLE_WITHDRAWN 회원은 자동 이동하지 않음
      if (resData.role === "ROLE_WITHDRAWN") {
        alert("탈퇴한 계정입니다. 복구 페이지로 이동합니다.");
        window.location.href = `http://localhost:5173/changestatus?mid=${resData.member.mid}`;
        return;
      }

      alert(`${resData.member.mid}님 환영합니다!`);
      navigate("/");
    } catch (error) {
      console.error("로그인 실패:", error);

      // 응답 상태별 처리 (302는 더 이상 안뜰 예정, 대신 401/400)
      if (error.response) {
        const { status, data } = error.response;

        // 423 Locked → 탈퇴/휴면 상태 안내
        if (status === 423 && data?.data) {
          alert(data.message || "휴면 또는 탈퇴한 계정입니다.");
          // 백엔드에서 전달한 URL로 이동
          window.location.href = data.data;
          return;
        }

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
      {/* 제목 */}
      <Typography
        variant="h5"
        sx={{ mb: 3, color: "#CA2E26", fontWeight: "bold" }}
      >
        🔥 Phoenix 로그인
      </Typography>

      {/* 로그인 폼 */}
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

      {/* 아이디 / 비밀번호 찾기 */}
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          gap: 2,
          mt: 2,
          fontSize: "0.9rem",
        }}
      >
        <Link
          component="button"
          underline="hover"
          sx={{
            color: "gray",
            "&:hover": { color: "#CA2E26" },
          }}
          onClick={() => navigate("/find-id")}
        >
          아이디 찾기
        </Link>

        <Typography sx={{ color: "#ccc" }}>|</Typography>

        <Link
          component="button"
          underline="hover"
          sx={{
            color: "gray",
            "&:hover": { color: "#CA2E26" },
          }}
          onClick={() => navigate("/find-pwd")}
        >
          비밀번호 찾기
        </Link>
      </Box>

      {/* 안내문 스타일 */}
      <Typography
        variant="body2"
        sx={{
          mt: 5,
          mb: 2,
          color: "#666",
          fontSize: "0.95rem",
          fontWeight: 500,
          letterSpacing: "0.3px",
        }}
      >
        SNS 계정으로{" "}
        <span style={{ color: "#CA2E26", fontWeight: "bold" }}>빠르게</span>{" "}
        로그인하세요
      </Typography>

      {/* 소셜 로그인 버튼 */}
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          gap: 4,
          mt: 2,
        }}
      >
        {/* Google */}
        <Button
          onClick={() => handleSocialLogin("google")}
          sx={{
            minWidth: 64,
            height: 64,
            borderRadius: "50%",
            bgcolor: "white",
            boxShadow: 2,
            "&:hover": { boxShadow: 4 },
          }}
        >
          <img src="/구글로고.jpg" alt="Google Login" style={{ width: 36, height: 36 }} />
        </Button>


        {/* GitHub */}
        <Button
          onClick={() => handleSocialLogin("github")}
          sx={{
            width: 64,
            height: 64,
            borderRadius: "50%",
            boxShadow: 2,
            bgcolor: "white",
            "&:hover": { boxShadow: 4 },
          }}
        >
          <img
            src="/깃로고.png"
            alt="GitHub Login"
            style={{ width: 36, height: 36 }}
          />
        </Button>

        {/* Facebook */}
        <Button
          onClick={() => handleSocialLogin("facebook")}
          sx={{
            width: 64,
            height: 64,
            borderRadius: "50%",
            boxShadow: 2,
            bgcolor: "#white",
            "&:hover": { boxShadow: 4 },
          }}
        >
          <img
            src="/페북로고.png"
            alt="Facebook Login"
            style={{ width: 36, height: 36 }}
          />
        </Button>
      </Box>
    </div>
  );
};

export default LoginForm;
