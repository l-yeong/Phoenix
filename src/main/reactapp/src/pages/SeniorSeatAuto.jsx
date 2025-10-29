import React, { useState, useEffect } from "react";
import {
    Box,
    Typography,
    FormControl,
    Select,
    MenuItem,
    Button,
} from "@mui/material";
import { useSearchParams } from "react-router-dom";
import styles from "../styles/SeniorSeatAuto.module.css";
import TutorialOverlay from "../components/TutorialOverlay";
import axios from "axios";

export default function SeniorSeatAuto() {
    const [searchParams] = useSearchParams();
    const [game, setGame] = useState(null);
    const [loading, setLoading] = useState(true);
    const [ticketCount, setTicketCount] = useState(1);
    const [guideStep, setGuideStep] = useState(0);
    const [recognition, setRecognition] = useState(null);
    const [listening, setListening] = useState(false);

    const gameId = searchParams.get("gameId");

    // 음성 안내 (TTS)
    const speak = (text) => {
        window.speechSynthesis.cancel();
        const utter = new SpeechSynthesisUtterance(text);
        utter.lang = "ko-KR";
        utter.rate = 0.9;
        utter.pitch = 1.0;
        utter.volume = 1.0;
        window.speechSynthesis.speak(utter);
    };

    // 음성 인식 (STT) 초기화
    const initSTT = async () => {
        const SpeechRecognition =
            window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            console.warn("이 브라우저는 음성 인식을 지원하지 않습니다.");
            return;
        }

        const recog = new SpeechRecognition();
        recog.lang = "ko-KR";
        recog.continuous = true;
        recog.interimResults = false;

        recog.onstart = () => {
            console.log("🎤 음성 인식 시작됨");
            setListening(true);
            speak("매수를 선택하시려면 한 장 또는 두 장이라고 말씀해주세요.");
        };

        recog.onresult = (event) => {
            const transcript = event.results[event.results.length - 1][0].transcript.trim();
            console.log("인식된 문장:", transcript);
            handleVoiceCommand(transcript);
        };

        recog.onerror = (err) => {
            console.error("음성 인식 오류:", err);
            setListening(false);
        };

        recog.onend = () => {
            console.log("인식 종료됨");
            setListening(false);
        };

        setRecognition(recog);
        recog.start();
    };

    // 음성 명령 처리
    const handleVoiceCommand = (text) => {
        const normalized = text.replace(/\s/g, "");

        if (normalized.includes("한") || normalized.includes("1")) {
            setTicketCount(1);
            speak("1매로 선택하셨습니다.");
            setTimeout(() => setGuideStep(2), 1500);
        } else if (normalized.includes("두") || normalized.includes("2")) {
            setTicketCount(2);
            speak("2매로 선택하셨습니다.");
            setTimeout(() => setGuideStep(2), 1500);
        } else if (
            normalized.includes("자동") ||
            normalized.includes("예매") ||
            normalized.includes("시작")
        ) {
            speak("자동 예매를 진행합니다.");
            setTimeout(() => handleAutoReserve(), 1000);
        } else if (normalized.includes("종료") || normalized.includes("나가기")) {
            speak("시니어 예매를 종료합니다.");
            if (recognition) recognition.stop();
        } else {
            speak("죄송합니다. 다시 말씀해주세요. 예를 들어 한 장 또는 두 장이라고 말해주세요.");
        }
    };

    // 경기 정보 불러오기 (데이터 → TTS → STT 순서)
    useEffect(() => {
        const fetchGame = async () => {
            try {
                const res = await axios.get(`http://localhost:8080/senior/games${gameId} `,
                    { withCredentials: true });
                if (res.data.success) {
                    setGame(res.data.data);
                    setLoading(false);

                    // 오버레이 표시
                    setTimeout(() => {
                        setGuideStep(1);

                        // TTS 안내
                        speak("매수를 선택해주세요. 몇 명이 예매할지 먼저 정해야 합니다.");

                        // TTS 후 STT 시작
                        setTimeout(() => {
                            initSTT();
                        }, 2500);
                    }, 800);
                } else {
                    alert("경기 정보를 불러오지 못했습니다.");
                    setLoading(false);
                }
            } catch (e) {
                console.log("경기 로드 실패:", e);
                setLoading(false);
            }
        };

        if (gameId) fetchGame();

        return () => {
            window.speechSynthesis.cancel();
            if (recognition) recognition.stop();
        };
    }, [gameId]);

    const handleAutoReserve = () => {
        // 현재 재생 중인 음성 종료
        window.speechSynthesis.cancel();

        // 음성 인식 중단 (인식 중이면)
        if (recognition) recognition.stop();

        // 음성 중단 후 약간의 딜레이
        setTimeout(() => {
            // 안내 후 예매 진행
            speak(`🎟️ ${ticketCount}매 자동 예매를 진행합니다.`);

            setTimeout(() => {
                alert(`🎟️ ${ticketCount}매 자동 예매를 진행합니다.`);
                // 추후 실제 API 연동
            }, 1200);
        }, 300); // cancel 후 딜레이 추가로 확실히 끊김 보장
    };

    return (
        <Box className={styles.container}>
            <Typography variant="h5" className={styles.title}>
                🎟️ 시니어 자동 좌석 배정
            </Typography>

            {loading ? (
                <Typography sx={{ mt: 4 }}>경기 정보를 불러오는 중...</Typography>
            ) : game ? (
                <>
                    <Typography
                        variant="h5"
                        sx={{ color: "#CA2E26", fontWeight: "bold", mb: 1 }}
                    >
                        {game.homeTeam} vs {game.awayTeam}
                    </Typography>
                    <Typography variant="subtitle1" sx={{ mb: 4 }}>
                        📅 {game.date} {game.time} / 🏟️ 인천 피닉스 파크
                    </Typography>
                </>
            ) : (
                <Typography sx={{ mt: 4 }}>경기 정보를 찾을 수 없습니다.</Typography>
            )}

            <Box className={styles.formWrapper}>
                <FormControl className={styles.selectBox}>
                    <Select
                        id="ticketSelectBox"
                        labelId="ticket-count-label"
                        value={ticketCount}
                        onChange={(e) => setTicketCount(e.target.value)}
                    >
                        <MenuItem value={1}>1매</MenuItem>
                        <MenuItem value={2}>2매</MenuItem>
                    </Select>
                </FormControl>

                <Button
                    id="autoReserveButton"
                    variant="contained"
                    className={styles.reserveButton}
                    onClick={handleAutoReserve}
                >
                    ⚾ 자동 예매하기
                </Button>
            </Box>

            {/* 튜토리얼 단계별 표시 */}
            {guideStep === 1 && (
                <TutorialOverlay
                    targetId="ticketSelectBox"
                    message={
                        <p style={{ textAlign: "center", lineHeight: "1.6" }}>
                            👥 <strong style={{ color: "#CA2E26" }}>매수를 선택해주세요.</strong>
                            <br />
                            몇 명이 예매할지 먼저 정해야 합니다.
                        </p>
                    }
                    onClose={() => {
                        window.speechSynthesis.cancel();
                        setGuideStep(2);

                        // 오버레이 닫으면 바로 다음 안내 이어지게
                        setTimeout(() => {
                            speak("이 버튼을 눌러 자동 예매를 진행해보세요.");
                        }, 300);
                    }}
                />
            )}

            {guideStep === 2 && (
                <TutorialOverlay
                    targetId="autoReserveButton"
                    message={
                        <p style={{ textAlign: "center", lineHeight: "1.6" }}>
                            🎟️ <strong style={{ color: "#CA2E26" }}>이 버튼을 눌러</strong>
                            <br />
                            자동 예매를 진행해보세요!
                        </p>
                    }
                    onClose={() => {
                        window.speechSynthesis.cancel();
                        setGuideStep(0);
                    }}
                />
            )}
        </Box>
    );
}
