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
import { useEffect } from "react";

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

  const [errors, setErrors] = useState({});
  const [emailCode, setEmailCode] = useState("");
  const [emailVerified, setEmailVerified] = useState(false);
  const [loading, setLoading] = useState(false);
  const [playerList, setPlayerList] = useState([]);
  const navigate = useNavigate();

  /** 정규식 패턴 */
  const regex = {
    mid: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{4,12}$/,
    password: /^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{8,20}$/, // 영문, 숫자, 특수문자 포함 8~20자
    email: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, // 이메일 형식
    phone: /^010-\d{4}-\d{4}$/, // 010-0000-0000 형식
    mname: /^[가-힣A-Za-z]{2,20}$/, // 한글 또는 영문 2~20자, 공백 불가
    birthdate: /^\d{4}-\d{2}-\d{2}$/, // 생년월일 YYYY-MM-DD 형식
  };

  /** 입력 변경 */
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === "checkbox" ? checked : value });
    if (errors[name]) validateField(name, value); // 실시간 유효성 업데이트
  };

  /** 필드별 유효성 검사 */
  const validateField = (name, value) => {
    let message = "";

    switch (name) {
      case "mid":
        if (!value.trim()) message = "아이디를 입력해주세요.";
        else if (!regex.mid.test(value))
          message = "아이디는 영문/숫자 4~12자여야 합니다.";
        break;

      case "password_hash":
        if (!value.trim()) message = "비밀번호를 입력해주세요.";
        else if (!regex.password.test(value))
          message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.";
        break;

      case "email":
        if (!value.trim()) message = "이메일을 입력해주세요.";
        else if (!regex.email.test(value)) message = "올바른 이메일 형식이 아닙니다.";
        break;

      case "mphone":
        if (!value.trim()) message = "전화번호를 입력해주세요.";
        else if (!regex.phone.test(value)) message = "전화번호는 010-0000-0000 형식으로 입력해주세요.";
        break;

      case "mname":
        if (!value.trim()) message = "이름을 입력해주세요.";
        else if (!regex.mname.test(value)) message = "이름은 한글 또는 영문으로 2~20자 이내여야 합니다.";
        break;

      case "birthdate":
        if (!value.trim()) message = "생년월일을 입력해주세요.";
        else if (!regex.birthdate.test(value))
          message = "생년월일은 YYYY-MM-DD 형식으로 입력해주세요.";
        break;

      default:
        break;
    }

    setErrors((prev) => ({ ...prev, [name]: message }));
    return message === "";
  };

  /** 전체 유효성 검사 */
  const validateAll = () => {
    const newErrors = {};

    if (!form.mid.trim())
      newErrors.mid = "아이디를 입력해주세요.";
    else if (!regex.mid.test(form.mid))
      newErrors.mid = "아이디는 영문/숫자 4~12자여야 합니다.";

    if (!form.password_hash.trim())
      newErrors.password_hash = "비밀번호를 입력해주세요.";
    else if (!regex.password.test(form.password_hash))
      newErrors.password_hash = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다.";

    if (!form.email.trim())
      newErrors.email = "이메일을 입력해주세요.";
    else if (!regex.email.test(form.email))
      newErrors.email = "올바른 이메일 형식이 아닙니다.";

    if (!form.mphone.trim())
      newErrors.mphone = "전화번호를 입력해주세요.";
    else if (!regex.phone.test(form.mphone))
      newErrors.mphone = "전화번호는 010-0000-0000 형식으로 입력해주세요.";

    if (!form.mname.trim())
      newErrors.mname = "이름을 입력해주세요";
    else if (!regex.mname.test(form.mname))
      newErrors.mname = "이름은 한글 또는 영문으로 2~20자 이내여야 합니다.";

    if (!form.birthdate.trim())
      newErrors.birthdate = "생년월일을 입력해주세요.";
    else if (!regex.birthdate.test(form.birthdate))
      newErrors.birthdate = "생년월일은 YYYY-MM-DD 형식으로 입력해주세요.";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /** 이메일 코드 전송 */
  const sendEmailCode = async () => {
    if (!regex.email.test(form.email)) {
      alert("올바른 이메일 형식을 입력해주세요.");
      return;
    }

    try {
      setLoading(true);
      const res = await api.post("/members/email/send", { email: form.email });
      if (res.data === true) {
        alert("인증코드가 이메일로 전송되었습니다!");
      } else {
        alert("이메일 전송 실패");
      }
    } catch (err) {
      alert("서버 오류로 이메일 전송에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  /** 이메일 인증 확인 */
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

  /** 회원가입 요청 */
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateAll()) {
      alert("입력 정보를 다시 확인해주세요.");
      return;
    }

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
        setTimeout(() => navigate("/login"), 1000);
      } else {
        alert("회원가입 실패");
      }
    } catch (err) {
      alert("회원가입 중 오류가 발생했습니다.");
    }
  };

  /** 서버에서 선수 목록 불러오기 */
  useEffect(() => {
    const fetchPlayers = async () => {
      try {
        const res = await api.get("/members/signup/players");
        if (res.data.success) {
          setPlayerList(res.data.data);
        } else {
          alert("선수 목록 불러오기 실패");
        }
      } catch (err) {
        console.error("선수 목록 로드 오류:", err);
      }
    };
    fetchPlayers();
  }, []);

  return (
    <div
      style={{
        textAlign: "center",
        marginTop: "80px",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
      }}
    >
      {/* 카드형 박스 */}
      <Box
        sx={{
          width: 500,
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
        {/* 제목 */}
        <Typography
          variant="h5"
          sx={{ mb: 3, color: "#CA2E26", fontWeight: "bold" }}
        >
          📝 Phoenix 회원가입
        </Typography>

        {/* 폼 영역 */}
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
          <TextField
            label="아이디"
            name="mid"
            value={form.mid}
            onChange={handleChange}
            onBlur={(e) => validateField("mid", e.target.value)}
            error={!!errors.mid}
            helperText={errors.mid}
            fullWidth
          />

          <TextField
            label="비밀번호"
            type="password"
            name="password_hash"
            value={form.password_hash}
            onChange={handleChange}
            onBlur={(e) => validateField("password_hash", e.target.value)}
            error={!!errors.password_hash}
            helperText={errors.password_hash}
            fullWidth
          />

          <TextField
            label="이름"
            name="mname"
            value={form.mname}
            onChange={handleChange}
            onBlur={(e) => validateField("mname", e.target.value)}
            error={!!errors.mname}
            helperText={errors.mname}
            fullWidth
          />

          <TextField
            label="전화번호 (010-0000-0000)"
            name="mphone"
            value={form.mphone}
            onChange={handleChange}
            onBlur={(e) => validateField("mphone", e.target.value)}
            error={!!errors.mphone}
            helperText={errors.mphone}
            fullWidth
          />

          <TextField
            label="생년월일"
            type="date"
            name="birthdate"
            value={form.birthdate}
            onChange={handleChange}
            onBlur={(e) => validateField("birthdate", e.target.value)}
            error={!!errors.birthdate}
            helperText={errors.birthdate}
            InputLabelProps={{ shrink: true }}
            fullWidth
          />

          {/* 이메일 + 코드 전송 */}
          <Box sx={{ display: "flex", gap: 1 }}>
            <TextField
              label="이메일"
              name="email"
              value={form.email}
              onChange={handleChange}
              onBlur={(e) => validateField("email", e.target.value)}
              error={!!errors.email}
              helperText={errors.email}
              fullWidth
              disabled={emailVerified}
            />
            <Button
              variant="outlined"
              onClick={sendEmailCode}
              disabled={loading || emailVerified}
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
              disabled={emailVerified}
            />
            <Button
              variant="outlined"
              onClick={verifyEmail}
              disabled={emailVerified}
            >
              {emailVerified ? "확인완료" : "인증확인"}
            </Button>
          </Box>

          <TextField
            select
            label="선호 선수"
            name="pno"
            value={form.pno}
            onChange={handleChange}
            fullWidth
          >
            {playerList.length > 0 ? (
              playerList.map((p) => (
                <MenuItem key={p.pno} value={p.pno}>
                  {p.name} ({p.position} · {p.teamName})
                </MenuItem>
              ))
            ) : (
              <MenuItem disabled>불러오는 중...</MenuItem>
            )}
          </TextField>

          <FormControlLabel
            control={
              <Checkbox
                checked={form.exchange}
                onChange={handleChange}
                name="exchange"
              />
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
              height: 55,
              fontSize: "1.1rem",
            }}
          >
            회원가입
          </Button>
          <Typography
            variant="body2"
            sx={{
              textAlign: "center",
              mt: 3,
              color: "#777",
              fontSize: "0.95rem",
            }}
          >
            이미 계정이 있으신가요?{" "}
            <span
              style={{
                color: "#CA2E26",
                fontWeight: "bold",
                cursor: "pointer",
              }}
              onClick={() => navigate("/login")}
            >
              로그인하기
            </span>
          </Typography>
        </Box>
      </Box>
    </div>
  );

};

export default SignUpPage;
