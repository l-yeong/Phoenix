import { useState } from "react";
import { Box, TextField, Button, Typography } from "@mui/material";
import api from "../api/axiosInstance";

const FindIdPage = () => {
  const [form, setForm] = useState({ mname: "", mphone: "", email: "" });
  const [code, setCode] = useState("");
  const [step, setStep] = useState(1);
  const [foundId, setFoundId] = useState("");

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const requestMail = async () => {
    const res = await api.post("/members/findid/request", form);
    if (res.data.success) setStep(2);
    alert(res.data.message);
  };

  const verifyCode = async () => {
    const res = await api.post("/members/findid/verify", {
      email: form.email,
      code,
    });
    if (res.data.success) {
      const result = await api.get(`/members/findid?email=${form.email}`);
      setFoundId(result.data.data);
      setStep(3);
    }
    alert(res.data.message);
  };

  return (
    <Box sx={{ width: 400, margin: "auto", mt: 10 }}>
      <Typography variant="h5" sx={{ mb: 3 }}>
        아이디 찾기
      </Typography>

      {step === 1 && (
        <>
          <TextField label="이름" name="mname" fullWidth sx={{ mb: 2 }} onChange={handleChange} />
          <TextField label="전화번호" name="mphone" fullWidth sx={{ mb: 2 }} onChange={handleChange} />
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
          찾은 아이디: <strong>{foundId}</strong>
        </Typography>
      )}
    </Box>
  );
};

export default FindIdPage;
