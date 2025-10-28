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
  const [games, setGames] = useState([]);

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

    /* 경기 목록 불러오기 함수 먼저 정의 */
    const fetchGames = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/senior/games` , {withCredentials : true});
        if (res.data.success) {
          setGames(res.data.data);
        } else {
          alert("경기 정보를 불러오지 못했습니다.");
        }
      } catch (e) {
        console.log("경기 로드 실패 : ", e);
      }
    };

    /* 접근 확인 함수 */
    const checkSeniorAccess = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/senior/reserve`, {
          withCredentials: true,
        });
        if (res.data.success) {
          await fetchGames(); // ⚡ 이제 정상적으로 호출 가능
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

  return (
    <Box className={styles.container}>
      <Typography variant="h3" className={styles.title}>
        ⚾ 시니어 자동 예매
      </Typography>
      <Typography variant="subtitle1" className={styles.subtitle}>
        예매를 원하는 경기를 선택해주세요.
      </Typography>


      <Box className={styles.cardContainer}>
        {games.length === 0 ? (
          <Typography sx={{ mt: 3 }}> 예매 가능한 경기가 없습니다. </Typography>
        ) : (
          games.map((game, idx) => (
            <Box
              key={game.gno}
              id={idx === 0 ? "firstGameButton" : undefined}
              className={styles.card}
            >
              <Typography className={styles.cardTitle}>
                {new Date(game.date).toLocaleDateString("ko-KR", {
                  month: "long",
                  day: "numeric",
                  weekday: "short",
                })}
              </Typography>
              <Typography className={styles.cardTeam}>
                {game.homeTeam} vs {game.awayTeam}
              </Typography>
              <Typography className={styles.cardPlace}>
                {game.place || "인천 피닉스 파크"}
              </Typography>
              <Button
                variant="contained"
                className={styles.cardButton}
                onClick={() => navigate(`/senior/seats?gameId=${game.gno}`)}
              >
                바로가기 →
              </Button>
            </Box>
          ))
        )
        }

      </Box>

      {showGuide && games.length > 0 && (
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
