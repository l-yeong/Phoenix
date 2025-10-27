import { useState } from "react";
import { Box, Typography, TextField, Button, styled } from "@mui/material";
import api from "../api/axiosInstance";

const FindPwdPage = () => {
  const [form, setForm] = useState({ mid: "", mname: "", email: "" });
  const [code, setCode] = useState("");
  const [step, setStep] = useState(1);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const requestMail = async () => {
    const res = await api.post("/members/findpwd/request", form);
    if (res.data.success) setStep(2);
    alert(res.data.message);
  };

  const verifyCode = async () => {
    const res = await api.post("/members/findpwd/verify", {
      email: form.email,
      code,
    });
    if (res.data.success) {
      await api.post("/members/findpwd/reset", { email: form.email });
      setStep(3);
    }
    alert(res.data.message);
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
      }}
    >
      <Typography
        variant="h5"
        sx={{ mb: 3, fontWeight: "bold", color: "#333", textAlign: "center" }}
      >
        비밀번호 찾기
      </Typography>

      {step === 1 && (
        <>
          <TextField
            label="아이디"
            name="mid"
            fullWidth
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
            onChange={handleChange}
          />
          <TextField
            label="이름"
            name="mname"
            fullWidth
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
            onChange={handleChange}
          />
          <TextField
            label="이메일"
            name="email"
            fullWidth
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
            onChange={handleChange}
          />
          <RedButton fullWidth onClick={requestMail}>
            인증메일 보내기
          </RedButton>
        </>
      )}

      {step === 2 && (
        <>
          <TextField
            label="인증 코드"
            fullWidth
            sx={{
              mb: 2,
              "& .MuiOutlinedInput-root": {
                borderRadius: 2,
                "&.Mui-focused fieldset": { borderColor: "#CA2E26" },
              },
            }}
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />
          <RedButton fullWidth onClick={verifyCode}>
            인증 코드 확인
          </RedButton>
        </>
      )}

      {step === 3 && (
        <Typography
          variant="h6"
          sx={{ mt: 4, textAlign: "center", color: "#CA2E26" }}
        >
          임시 비밀번호가 이메일로 발송되었습니다.
        </Typography>
      )}
    </Box>
  );
};

export default FindPwdPage;
