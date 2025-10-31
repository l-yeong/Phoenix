import React, { useState } from "react";
import {
  Box,
  Button,
  TextField,
  Typography,
  Link,
  styled
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
      const response = await api.post(
        "/members/login",
        { mid, password_hash: password },
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
        mname: resData.member.mname,
        role: resData.role,
        status: resData.member.status,
      });

      // ROLE_WITHDRAWN 회원은 자동 이동하지 않음
      if (resData.role === "ROLE_WITHDRAWN") {
        alert("탈퇴한 계정입니다. 복구 페이지로 이동합니다.");
        window.location.href = `http://192.168.40.190:5173/changestatus?mid=${resData.member.mid}`;
        return;
      }

      alert(`${resData.member.mname}님 환영합니다!`);
      navigate("/");
    } catch (error) {
      console.error("로그인 실패:", error);
      const { status, data } = error.response || {};
      if (status === 423 && data?.data) {
        alert(data.message || "휴면 또는 탈퇴한 계정입니다.");
        window.location.href = data.data;
      } else if (status === 401) {
        alert("인증되지 않은 요청입니다. 다시 로그인해주세요.");
      } else if (status === 400) {
        alert("아이디 또는 비밀번호를 확인해주세요.");
      } else {
        alert("서버 오류가 발생했습니다.");
      }
    }
  };

  /** 소셜 로그인 리디렉션 */
  const handleSocialLogin = (provider) => {
    window.location.href = `http://192.168.40.190:8080/oauth2/authorization/${provider}`;
  };

  const RedButton = styled(Button)({
    mt: 2,
    background: "linear-gradient(45deg, #CA2E26 30%, #FF4C4C 90%)",
    color: "white",
    fontWeight: "bold",
    borderRadius: 8,
    padding: "10px 0",
    transition: "all 0.3s ease",
    "&:hover": {
      background: "linear-gradient(45deg, #b22720 30%, #ff2a2a 90%)",
      transform: "scale(1.03)",
    },
  });

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


      {/* 로그인 폼 */}
       <Box
        sx={{
          width: 400,
          margin: "auto",
          mt: 10,
          p: 4,
          mb: 10,
          borderRadius: 3,
          boxShadow: 3,
          bgcolor: "#fafafa",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
        }}
      >
        <Typography
          variant="h5"
          sx={{
            mb: 3,
            fontWeight: "bold",
            color: "#CA2E26",
            textAlign: "center",
          }}
        >
          🔥 Phoenix 로그인
        </Typography>

        <Box component="form" onSubmit={handleLogin} sx={{ width: "100%" }}>
          <TextField
            label="아이디"
            fullWidth
            value={mid}
            onChange={(e) => setMid(e.target.value)}
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
          />
          <TextField
            label="비밀번호"
            type="password"
            fullWidth
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
          />
          <RedButton fullWidth type="submit">
            로그인
          </RedButton>
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
            sx={{ color: "gray", "&:hover": { color: "#CA2E26" } }}
            onClick={() => navigate("/find-id")}
          >
            아이디 찾기
          </Link>
          <Typography sx={{ color: "#ccc" }}>|</Typography>
          <Link
            component="button"
            underline="hover"
            sx={{ color: "gray", "&:hover": { color: "#CA2E26" } }}
            onClick={() => navigate("/find-pwd")}
          >
            비밀번호 찾기
          </Link>
        </Box>

        {/* 안내문 */}
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
            <img
              src="/구글로고.jpg"
              alt="Google Login"
              style={{ width: 50, height: 40 }}
            />
          </Button>

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

          <Button
            onClick={() => handleSocialLogin("facebook")}
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
              src="/페북로고.png"
              alt="Facebook Login"
              style={{ width: 36, height: 36 }}
            />
          </Button>
        </Box>
      </Box>
    </div>
  );
};

export default LoginForm;
