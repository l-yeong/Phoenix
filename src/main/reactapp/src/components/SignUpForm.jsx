import { useState } from "react";
import { Box, TextField, Button, Typography, FormControlLabel, Checkbox, MenuItem } from "@mui/material";
import api from "../api/axiosInstance";
import styles from "../styles/Auth.module.css";

const SignUpPage = () => {
  const [form, setForm] = useState({
    mid: "",
    password_hash: "",
    mname: "",
    mphone: "",
    email: "",
    birthdate: "",
    pno: "", // 선호 선수
    exchange: false, // 예매 교환 여부
  });
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  // 이메일 인증 코드 전송
  const sendEmailCode = async () => {
    try {
      setLoading(true);
      const response = await api.post("/members/email/send", { email: form.email });
      alert(response.data ? "인증코드가 이메일로 전송되었습니다." : "전송 실패");
    } catch (err) {
      alert("이메일 전송 실패");
    } finally {
      setLoading(false);
    }
  };

  // 인증 코드 확인
  const verifyEmail = async () => {
    try {
      const response = await api.post("/members/verify-email", {
        email: form.email,
        code: emailCode,
      });
      if (response.data.success) {
        alert("이메일 인증 완료!");
        setEmailVerified(true);
      } else {
        alert("인증 실패");
      }
    } catch (err) {
      alert("인증 실패");
    }
  };

  // 회원가입 요청
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!emailVerified) {
      alert("이메일 인증을 먼저 완료해주세요.");
      return;
    }

    try {
      const response = await api.post("/members/signup", {
        ...form,
        provider: null,
        provider_id: null,
        status: "active",
        email_verified: true,
      });
      if (response.data.success) {
        alert("회원가입 성공!");
      } else {
        alert("회원가입 실패");
      }
    } catch (err) {
      alert("회원가입 중 오류 발생");
    }
  };

  return (
    <Box className={styles.container}>
      <Typography variant="h4" mb={3}>회원가입</Typography>

      <TextField fullWidth label="아이디" name="mid" value={form.mid} onChange={handleChange} margin="normal" />
      <TextField fullWidth label="비밀번호" type="password" name="password_hash" value={form.password_hash} onChange={handleChange} margin="normal" />
      <TextField fullWidth label="이름" name="mname" value={form.mname} onChange={handleChange} margin="normal" />
      <TextField fullWidth label="전화번호" name="mphone" value={form.mphone} onChange={handleChange} margin="normal" />
      <TextField fullWidth label="생년월일" name="birthdate" type="date" value={form.birthdate} onChange={handleChange} margin="normal" InputLabelProps={{ shrink: true }} />

      {/* 이메일 + 인증 */}
      <Box display="flex" gap={2} alignItems="center" mt={2}>
        <TextField fullWidth label="이메일" name="email" value={form.email} onChange={handleChange} />
        <Button onClick={sendEmailCode} disabled={loading}>인증코드 전송</Button>
      </Box>

      <Box display="flex" gap={2} alignItems="center" mt={2}>
        <TextField fullWidth label="인증코드 입력" value={emailCode} onChange={(e) => setEmailCode(e.target.value)} />
        <Button onClick={verifyEmail}>인증확인</Button>
      </Box>

      {/* 선호 선수 / 교환 여부 */}
      <TextField
        select
        fullWidth
        label="선호 선수"
        name="pno"
        value={form.pno}
        onChange={handleChange}
        margin="normal"
      >
        <MenuItem value="1">선수1</MenuItem>
        <MenuItem value="2">선수2</MenuItem>
        <MenuItem value="3">선수3</MenuItem>
      </TextField>

      <FormControlLabel
        control={<Checkbox checked={form.exchange} onChange={handleChange} name="exchange" />}
        label="예매 교환 가능"
      />

      <Button
        fullWidth
        variant="contained"
        color="primary"
        onClick={handleSubmit}
        sx={{ mt: 3 }}
      >
        회원가입
      </Button>
    </Box>
  );
};

export default SignUpPage;
