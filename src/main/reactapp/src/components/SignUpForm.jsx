import React, { useState } from "react";
import {
  TextField,
  Button,
  MenuItem,
  Typography,
  FormControlLabel,
  Checkbox,
  Box,
} from "@mui/material";
import api from "../api/axiosInstance";
import { useNavigate } from "react-router-dom";

const SignUpPage = () => {
  const [form, setForm] = useState({
    mid: "",
    password_hash: "",
    mname: "",
    mphone: "",
    email: "",
    birthdate: "",
    pno: "",
    exchange: false,
  });
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const playerList = [
    { id: 1, name: "박찬호" },
    { id: 2, name: "류현진" },
    { id: 3, name: "이정후" },
    { id: 4, name: "오타니 쇼헤이" },
    { id: 5, name: "추신수" },
    { id: 6, name: "김하성" },
  ];

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
  };

  // 이메일 인증 코드 전송
  const sendEmailCode = async () => {
    if (!form.email) {
      alert("이메일을 입력해주세요.");
      return;
    }

    try {
      setLoading(true);
      const res = await api.post("/members/email/send", { email: form.email });
      console.log("이메일 응답:", res.data);

      if (res.data === true) {
        alert("인증코드가 이메일로 전송되었습니다!");
      } else {
        alert("이메일 전송 실패");
      }
    } catch (err) {
      console.error("이메일 전송 오류:", err);
      alert("서버 오류로 이메일 전송에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // 인증 코드 확인
  const verifyEmail = async () => {
    try {
      const res = await api.post("/members/verify-email", {
        email: form.email,
        code: emailCode,
      });
      if (res.data.success) {
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
      alert("이메일 인증을 완료해주세요.");
      return;
    }

    try {
      const res = await api.post("/members/signup", {
        ...form,
        provider: null,
        provider_id: null,
        status: "active",
        email_verified: true,
      });
      if (res.data.success) {
        alert("회원가입 성공!");
        // 2초 뒤 자동 이동
        setTimeout(() => navigate("/login"), 1000);
      } else {
        alert("회원가입 실패");
      }
    } catch (err) {
      alert("회원가입 중 오류가 발생했습니다.");
    }
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
      <Typography
        variant="h5"
        sx={{ mb: 3, color: "#CA2E26", fontWeight: "bold" }}
      >
        📝 회원가입
      </Typography>

      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{
          display: "flex",
          flexDirection: "column",
          gap: 2,
          width: "100%",
          maxWidth: "400px",
        }}
      >
        <TextField label="아이디" name="mid" value={form.mid} onChange={handleChange} fullWidth />
        <TextField
          label="비밀번호"
          type="password"
          name="password_hash"
          value={form.password_hash}
          onChange={handleChange}
          fullWidth
        />
        <TextField label="이름" name="mname" value={form.mname} onChange={handleChange} fullWidth />
        <TextField label="전화번호" name="mphone" value={form.mphone} onChange={handleChange} fullWidth />
        <TextField
          label="생년월일"
          type="date"
          name="birthdate"
          value={form.birthdate}
          onChange={handleChange}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />

        {/* 이메일 + 인증 */}
        <Box sx={{ display: "flex", gap: 1 }}>
          <TextField
            label="이메일"
            name="email"
            value={form.email}
            onChange={handleChange}
            fullWidth
          />
          <Button
            variant="outlined"
            onClick={sendEmailCode}
            disabled={loading}
            sx={{ whiteSpace: "nowrap" }}
          >
            코드전송
          </Button>
        </Box>

        <Box sx={{ display: "flex", gap: 1 }}>
          <TextField
            label="인증코드 입력"
            value={emailCode}
            onChange={(e) => setEmailCode(e.target.value)}
            fullWidth
          />
          <Button variant="outlined" onClick={verifyEmail}>
            인증확인
          </Button>
        </Box>

        {/* 선호 선수 / 교환 여부 */}
        <TextField
          select
          label="선호 선수"
          name="pno"
          value={form.pno}
          onChange={handleChange}
          fullWidth
        >
          {playerList.map((p) => (
            <MenuItem key={p.id} value={p.id}>
              {p.name}
            </MenuItem>
          ))}
        </TextField>

        <FormControlLabel
          control={
            <Checkbox checked={form.exchange} onChange={handleChange} name="exchange" />
          }
          label="예매 교환 가능"
        />

        <Button
          variant="contained"
          type="submit"
          sx={{
            mt: 2,
            bgcolor: "#CA2E26",
            color: "white",
            fontWeight: "bold",
            "&:hover": { bgcolor: "#b22720" },
          }}
        >
          회원가입
        </Button>
      </Box>
    </div>
  );
};

export default SignUpPage;
