import React, { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { TextField, Button, MenuItem, Typography, FormControlLabel, Switch } from "@mui/material";
import api from "../api/axiosInstance";

const SocialSignUp = () => {
  const [params] = useSearchParams();
  const [email, setEmail] = useState("");
  const [favoritePlayer, setFavoritePlayer] = useState("");
  const [exchangeEligible, setExchangeEligible] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    setEmail(params.get("email") || "");
  }, [params]);

  const playerList = [
    { id: 1, name: "박찬호" },
    { id: 2, name: "류현진" },
    { id: 3, name: "이정후" },
    { id: 4, name: "오타니 쇼헤이" },
    { id: 5, name: "추신수" },
    { id: 6, name: "김하성" },
  ];

  const handleSubmit = async () => {
    if (!favoritePlayer) {
      alert("선호 선수를 선택해주세요.");
      return;
    }

    try {
      //  DTO 구조에 맞게 JSON 전송
      const res = await api.post("/members/social/signup", {
        email,
        provider: params.get("provider"),
        provider_id: params.get("provider_id"),
        pno: favoritePlayer,
        exchange: exchangeEligible,
      });

      if (res.data.success) {
        alert(res.data.message || "회원가입 완료!");
        navigate("/login"); // 로그인 페이지나 메인으로 이동
      } else {
        alert(res.data.message || "회원가입 실패");
      }
    } catch (err) {
      console.error(err);
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

      <TextField
        select
        label="선호 선수"
        value={favoritePlayer}
        onChange={(e) => setFavoritePlayer(e.target.value)}
        fullWidth
        sx={{ maxWidth: "400px", mb: 3 }}
      >
        {playerList.map((player) => (
          <MenuItem key={player.id} value={player.id}>
            {player.name}
          </MenuItem>
        ))}
      </TextField>

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
