import React, { useEffect, useState, useMemo } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { TextField, Button, MenuItem, Typography, FormControlLabel, Switch, CircularProgress } from "@mui/material";
import api from "../api/axiosInstance";

const SocialSignUp = () => {
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [favoritePlayer, setFavoritePlayer] = useState(0);
  const [exchangeEligible, setExchangeEligible] = useState(false);
  const [players, setPlayers] = useState([]);         // [{pno, pname}, ...]
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const navigate = useNavigate();

  useEffect(() => {
    setEmail(params.get("email") || "");
  }, [params]);

  // /game/players 로드
  useEffect(() => {
    let ignore = false;
    (async () => {
      setLoading(true); setErr("");
      try {
        const res = await api.get("/game/players");
        const raw = Array.isArray(res.data?.data) ? res.data.data : [];
        const normalized = raw.map(row => ({
          pno: Number(row.pno),
          pname: row.name,           // 서버 필드명이 name
          teamName: row.teamName,
          position: row.position,
        }));
        if (!ignore) setPlayers(normalized);
      } catch (e) {
        if (!ignore) setErr("선수 목록을 불러오지 못했습니다.");
        console.error("GET /game/players 실패:", e);
      } finally {
        if (!ignore) setLoading(false);
      }
    })();
    return () => { ignore = true; };
  }, []);

  const provider = useMemo(() => params.get("provider") || "", [params]);
  const provider_id = useMemo(() => params.get("provider_id") || "", [params]);

  const handleSubmit = async () => {
    if (!favoritePlayer) {
      alert("선호 선수를 선택해주세요.");
      return;
    }
    try {
      const res = await api.post("/members/social/signup", {
        email,
        provider,
        provider_id,
        pno: Number(favoritePlayer),   // 서버가 숫자 기대 시 변환
        exchange: exchangeEligible,
      });
      if (res.data?.success) {
        alert(res.data?.message || "회원가입 완료!");
        navigate("/login");
      } else {
        alert(res.data?.message || "회원가입 실패");
      }
    } catch (err) {
      console.error("소셜 회원가입 오류:", err);
      const status = err?.response?.status;

      // 이미 가입된 회원 처리
      if (status === 400) {
        alert("이미 가입된 회원입니다. 로그인 페이지로 이동합니다.");
        navigate("/login");
        return;
      }

      alert("서버 요청 중 오류가 발생했습니다.");
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
      <Typography variant="h5" sx={{ mb: 3 }}>
        ⚾ 추가 정보 입력
      </Typography>

      <TextField
        label="이메일"
        value={email}
        disabled
        fullWidth
        sx={{ maxWidth: "400px", mb: 2 }}
      />

      <div style={{ width: "100%", maxWidth: 400 }}>
        {loading ? (
          <div style={{ display: "flex", alignItems: "center", gap: 8, margin: "12px 0" }}>
            <CircularProgress size={20} />
            <span>선수 목록 불러오는 중…</span>
          </div>
        ) : (
          <TextField select label="선호 선수" value={favoritePlayer}
            onChange={(e) => setFavoritePlayer(Number(e.target.value))}
            fullWidth sx={{ mb: 3 }}>
            {players.length === 0 && <MenuItem value="" disabled>선수 데이터가 없습니다</MenuItem>}
            {players.map(p => (
              <MenuItem key={p.pno} value={p.pno}>
                {p.pname} ({p.teamName} / {p.position})
              </MenuItem>
            ))}
          </TextField>
        )}
        {err && <Typography color="error" sx={{ mb: 1 }}>{err}</Typography>}
      </div>

      <FormControlLabel
        control={
          <Switch
            checked={exchangeEligible}
            onChange={(e) => setExchangeEligible(e.target.checked)}
            color="primary"
          />
        }
        label="좌석 교환 신청 여부"
        sx={{ mb: 3 }}
      />

      <Button
        variant="contained"
        onClick={handleSubmit}
        disabled={loading || players.length === 0}
        sx={{
          bgcolor: "#CA2E26",
          color: "white",
          fontWeight: "bold",
          width: "200px",
          "&:hover": { bgcolor: "#b22720" },
        }}
      >
        가입 완료
      </Button>
    </div>
  );
};

export default SocialSignUp;
