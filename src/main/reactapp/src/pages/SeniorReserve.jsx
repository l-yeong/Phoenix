import React, { useState, useEffect } from "react";
import { Box, Typography, Button, CircularProgress } from "@mui/material";
import styles from "../styles/SeniorReserve.module.css";
import { useNavigate } from "react-router-dom";
import TutorialOverlay from "../components/TutorialOverlay";
import axios from "axios";

export default function SeniorReserve() {
  const navigate = useNavigate();
  const [showGuide, setShowGuide] = useState(false);
  const [loading, setLoading] = useState(true);

  const speak = (text) => {
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = "ko-KR";
    utter.rate = 0.9;
    utter.pitch = 1.0;
    utter.volume = 1.0;
    window.speechSynthesis.speak(utter);
  };

  useEffect(() => {
    const checkSeniorAccess = async () => {
      try {
        const res = await axios.get(`http://192.168.40.190:8080/senior/reserve`, {
          withCredentials: true,
        });
        if (res.data.success) {
          setShowGuide(true);
          speak("이 버튼을 눌러 예매할 경기를 선택해보세요.");
        }
      } catch (err) {
        const status = err.response?.status;
        if (status === 401) {
          alert("로그인이 필요합니다.");
          navigate("/login");
        } else if (status === 403) {
          alert("시니어 전용 서비스입니다. 65세 이상 회원만 이용 가능합니다.");
          navigate("/");
        } else {
          alert("접근이 거부되었습니다.");
          navigate("/");
        }
      } finally {
        setLoading(false);
      }
    };
    checkSeniorAccess();
    return () => window.speechSynthesis.cancel();
  }, []);

  if (loading)
    return (
      <Box sx={{ textAlign: "center", mt: 10 }}>
        <CircularProgress />
        <Typography sx={{ mt: 2 }}>접근 권한 확인 중...</Typography>
      </Box>
    );

  const games = [
    { id: 1, date: "10월 29일 (화)", teams: "PHOENIX vs LIONS", place: "서울구장" },
    { id: 2, date: "10월 30일 (수)", teams: "PHOENIX vs DRAGONS", place: "부산구장" },
    { id: 3, date: "11월 1일 (금)", teams: "PHOENIX vs BEARS", place: "대전구장" },
  ];

  return (
    <Box className={styles.container}>
      <Typography variant="h3" className={styles.title}>
        ⚾ 시니어 자동 예매
      </Typography>
      <Typography variant="subtitle1" className={styles.subtitle}>
        예매를 원하는 경기를 선택해주세요.
      </Typography>

      <Box className={styles.cardContainer}>
        {games.map((game, idx) => (
          <Box
            key={game.id}
            id={idx === 0 ? "firstGameButton" : undefined}
            className={styles.card}
          >
            <Typography className={styles.cardTitle}>{game.date}</Typography>
            <Typography className={styles.cardTeams}>{game.teams}</Typography>
            <Typography className={styles.cardPlace}>({game.place})</Typography>
            <Button
              variant="contained"
              className={styles.cardButton}
              onClick={() => navigate(`/senior/seats?gameId=${game.id}`)}
            >
              바로가기 →
            </Button>
          </Box>
        ))}
      </Box>

      {showGuide && (
        <TutorialOverlay
          targetId="firstGameButton"
          message={
            <p style={{ textAlign: "center", lineHeight: "1.6" }}>
              ⚾ <strong style={{ color: "#CA2E26" }}>이 버튼을 눌러</strong>
              <br />
              예매할 경기를 선택해보세요!
            </p>
          }
          onClose={() => {
            window.speechSynthesis.cancel();
            setShowGuide(false);
          }}
        />
      )}
    </Box>
  );
}
