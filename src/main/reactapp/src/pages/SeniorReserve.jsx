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
  const [listening, setListening] = useState(false);
  const [recognition, setRecognition] = useState(null);

  // 음성 안내 함수 (TTS)
  const speak = (text) => {
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = "ko-KR";
    utter.rate = 0.9;
    utter.pitch = 1.0;
    utter.volume = 1.0;
    window.speechSynthesis.speak(utter);
  };

  // 음성 인식 초기화
  const initSTT = () => {
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("이 브라우저에서는 음성 인식을 지원하지 않습니다.");
      return;
    }

    const recog = new SpeechRecognition();
    recog.lang = "ko-KR";
    recog.continuous = true;
    recog.interimResults = false;

    recog.onstart = () => {
      console.log(" 음성 인식 시작");
      setListening(true);
      speak("예매할 경기를 말해주세요. 예를 들어, 첫 번째 경기 선택이라고 말씀하세요.");
    };

    recog.onresult = (event) => {
      const transcript = event.results[event.results.length - 1][0].transcript.trim();
      console.log("🎙 인식된 문장:", transcript);
      handleVoiceCommand(transcript);
    };

    recog.onerror = (err) => {
      console.error("음성 인식 오류:", err);
      setListening(false);
    };

    recog.onend = () => {
      console.log("🎤 인식 종료됨");
      setListening(false);
    };

    setRecognition(recog);
  };

  // 음성 명령 처리 함수
  const handleVoiceCommand = (text) => {
    // "첫 번째 경기", "두번째 경기", "1번 경기", 등 인식 가능하도록
    const normalized = text.replace(/\s/g, "");

    if (normalized.includes("첫") || normalized.includes("1")) {
      navigateToGame(0);
    } else if (normalized.includes("두") || normalized.includes("2")) {
      navigateToGame(1);
    } else if (normalized.includes("세") || normalized.includes("3")) {
      navigateToGame(2);
    } else if (normalized.includes("종료") || normalized.includes("나가기")) {
      speak("시니어 예매를 종료합니다.");
      recognition.stop();
      navigate("/");
    } else {
      speak("죄송합니다. 다시 말씀해주세요. 예를 들어 첫 번째 경기 선택이라고 말해주세요.");
    }
  };

  const navigateToGame = (index) => {
    if (games[index]) {
      speak(`${games[index].homeTeam} 대 ${games[index].awayTeam} 경기를 선택하셨습니다.`);
      setTimeout(() => {
        navigate(`/senior/seats?gameId=${games[index].gno}`);
      }, 1500);
    } else {
      speak("해당 순서의 경기를 찾을 수 없습니다.");
    }
  };

  // 초기 실행
  useEffect(() => {
    const fetchGames = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/senior/games`, {
          withCredentials: true,
        });
        if (res.data.success) {
          setGames(res.data.data);
        } else {
          alert("경기 정보를 불러오지 못했습니다.");
        }
      } catch (e) {
        console.log("경기 로드 실패:", e);
      }
    };

    const checkSeniorAccess = async () => {
      try {
        const res = await axios.get(`http://localhost:8080/senior/reserve`, {
          withCredentials: true,
        });
        if (res.data.success) {
          await fetchGames();                 // 경기 목록 불러오기
          setShowGuide(true);                 // 화면에 오버레이 표시

          speak("시니어 전용 자동 예매 페이지입니다. 곧 음성 안내가 시작됩니다."); // 첫 음성 안내

          // 약간의 텀을 두고 두 번째 안내 + 음성인식 시작
          setTimeout(() => {
            speak("음성으로도 경기 선택이 가능합니다. 첫 번째 경기 선택이라고 말씀해보세요.");
            initSTT(); // 음성 인식 기능 시작
          }, 3000);
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

    return () => {
      window.speechSynthesis.cancel();
      if (recognition) recognition.stop();
    };
  }, []);

  // 음성 인식 시작 버튼 (테스트용)
  const startListening = () => {
    if (recognition && !listening) recognition.start();
  };

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
        예매를 원하는 경기를 말하거나 선택해주세요.
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
              <Typography className={styles.cardTeams}>
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
        )}
      </Box>

      {/* STT 수동 시작용 버튼 (디버그용) */}
      <Button
        onClick={startListening}
        variant="outlined"
        sx={{ mt: 3 }}
      >
        음성인식 {listening ? "진행 중..." : "시작하기"}
      </Button>

      {showGuide && games.length > 0 && (
        <TutorialOverlay
          targetId="firstGameButton"
          message={
            <p style={{ textAlign: "center", lineHeight: "1.6" }}>
              ⚾ <strong style={{ color: "#CA2E26" }}>음성으로도 </strong> 경기 선택이 가능합니다.
              <br />
              “첫 번째 경기 선택”이라고 말해보세요.
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
