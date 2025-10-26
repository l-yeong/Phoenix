import React, { useState, useEffect } from "react";
import { Box, Typography, Button } from "@mui/material";
import styles from "../styles/SeniorReserve.module.css";
import { useNavigate } from "react-router-dom";
import TutorialOverlay from "../components/TutorialOverlay";

export default function SeniorReserve() {
  const navigate = useNavigate();
  const [showGuide, setShowGuide] = useState(true);

  // 버튼 렌더링 후 오버레이 띄우기
  useEffect(() => {
    const timer = setTimeout(() => setShowGuide(true), 200);
    return () => clearTimeout(timer);
  }, []);

  const games = [
    { id: 1, date: "10월 29일 (화)", teams: "PHOENIX vs LIONS", place: "서울구장" },
    { id: 2, date: "10월 30일 (수)", teams: "PHOENIX vs DRAGONS", place: "부산구장" },
    { id: 3, date: "11월 1일 (금)", teams: "PHOENIX vs BEARS", place: "대전구장" },
  ];

  return (
    <Box className={styles.container}>
      <Typography variant="h4" className={styles.title}>
        ⚾ 시니어 자동 예매
      </Typography>
      <Typography variant="subtitle1" className={styles.subtitle}>
        3일 내 예매 가능한 경기를 선택해주세요.
      </Typography>

      <Box className={styles.buttonList}>
        {games.map((game, idx) => (
          <Button
            key={game.id}
            id={idx === 0 ? "firstGameButton" : undefined}
            variant="contained"
            className={styles.gameButton}
            onClick={() => navigate(`/senior/seats?gameId=${game.id}`)} // 수정된 경로
          >
            {game.date} <br /> {game.teams} <br /> ({game.place})
          </Button>
        ))}
      </Box>

      {showGuide && (
        <TutorialOverlay
          targetId="firstGameButton"
          message={
            <p style={{ textAlign: "center", lineHeight: "1.6" }}>
              ⚾ <strong style={{ color: "#CA2E26" }}>이 버튼을 눌러</strong><br />
              예매할 경기를 선택해보세요!
            </p>
          }
          onClose={() => setShowGuide(false)}
        />
      )}
    </Box>
  );
}
