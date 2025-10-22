import { useState } from "react";
import { Box, TextField, Button, Typography } from "@mui/material";
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

  return (
    <Box sx={{ width: 400, margin: "auto", mt: 10 }}>
      <Typography variant="h5" sx={{ mb: 3 }}>
        비밀번호 찾기
      </Typography>

      {step === 1 && (
        <>
          <TextField label="아이디" name="mid" fullWidth sx={{ mb: 2 }} onChange={handleChange} />
          <TextField label="이름" name="mname" fullWidth sx={{ mb: 2 }} onChange={handleChange} />
          <TextField label="이메일" name="email" fullWidth sx={{ mb: 2 }} onChange={handleChange} />
          <Button variant="contained" fullWidth onClick={requestMail}>
            인증메일 보내기
          </Button>
        </>
      )}

      {step === 2 && (
        <>
          <TextField label="인증 코드" fullWidth sx={{ mb: 2 }} value={code} onChange={(e) => setCode(e.target.value)} />
          <Button variant="contained" fullWidth onClick={verifyCode}>
            인증 코드 확인
          </Button>
        </>
      )}

      {step === 3 && (
        <Typography variant="h6" sx={{ mt: 4 }}>
          임시 비밀번호가 이메일로 발송되었습니다.
        </Typography>
      )}
    </Box>
  );
};

export default FindPwdPage;
