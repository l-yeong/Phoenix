import React, { useState, useEffect, useRef } from "react";
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
    const firstStart = useRef(true);
    const sttRestarting = useRef(false); // STT 중복 재시작 방지용 플래그
    const ttsActive = useRef(false); // 현재 TTS가 동작 중인지 추적

    const gameId = searchParams.get("gameId");

    // TTS 함수
    const speak = (text, autoListen = true) => {
        // 현재 재생 중인 음성 중단
        window.speechSynthesis.cancel();

        const utter = new SpeechSynthesisUtterance(text);
        utter.lang = "ko-KR";
        utter.rate = 0.9;
        utter.pitch = 1.0;
        utter.volume = 1.0;

        ttsActive.current = true; // 🎙️ TTS 활성화 시작
        window.speechSynthesis.speak(utter);

        utter.onend = () => {
            ttsActive.current = false; // 🎙️ TTS 종료 표시
            console.log("🎤 안내 종료됨 (TTS 완전 종료)");

            // 자동으로 STT 재시작
            if (autoListen && recognition && !listening && !sttRestarting.current) {
                sttRestarting.current = true;

                // 완전 종료 후 2.5초 이상 대기 (충돌 방지)
                setTimeout(() => {
                    // 혹시 그 사이에 TTS가 다시 시작됐으면 취소
                    if (ttsActive.current) {
                        sttRestarting.current = false;
                        return;
                    }

                    try {
                        recognition.start();
                        console.log("🎤 STT 완전 재시작됨 ");
                    } catch (err) {
                        console.error("🎤 STT 재시작 오류:", err);
                    } finally {
                        sttRestarting.current = false;
                    }
                }, 2500);
            }
        };
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

            // 처음 한번만 안내
            if (firstStart.current) {
                speak("매수를 선택하시려면 한 장 또는 두 장이라고 말씀해주세요.");
                firstStart.current = false;
            }
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
            console.log("🎤 인식 종료됨");
            setListening(false);

            // 자동예매 안내 중 끊겼을 때만 복구하되, 
            // TTS(onend) 이벤트보다 늦게 실행되게 1.5초 이상 딜레이 줌
            if (guideStep === 2) {
                setTimeout(() => {
                    try {
                        recognition.start(); // recog 대신 recognition (state에 저장된 최신 객체)
                        console.log("🎤 자동예매 단계에서 STT 안정 복구됨");
                    } catch (err) {
                        console.warn("STT 복구 실패:", err);
                    }
                }, 1800); // 기존 1000 → 1800ms로 변경 (TTS 충돌 방지)
            }
        };

        setRecognition(recog);
        recog.start();
    };

    // 음성 명령 처리
    const handleVoiceCommand = (text) => {
        const normalized = text.replace(/\s/g, "");

        if (normalized.includes("한") || normalized.includes("1")) {

            setTicketCount(1);

            // STT 자동 재시작 막기( false )
            speak("1매로 선택하셨습니다.");

            // 약간의 텀을 두고 다음 안내 STT 다시 활성화
            setTimeout(() => {
                setGuideStep(2);
                speak("이제 자동 예매 버튼을 눌러보세요.", true); // 다음 TTS 끝나면 STT 재시작
            }, 2000);

        } else if (normalized.includes("두") || normalized.includes("2")) {
            setTicketCount(2);
            speak("2매로 선택하셨습니다.");

            // 여기도 약간 텀 두고 다음 안내 STT 다시 활성화
            setTimeout(() => {
                setGuideStep(2);
                speak("이제 자동 예매 버튼을 눌러보세요.", true);
            }, 2000);

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

                    // 오버레이 표시 후 약간의 텀 두고 안내
                    setTimeout(() => {
                        setGuideStep(1);

                        // STT 미리 초기화 해두기
                        initSTT();

                        // TTS 안내 시작 - 안내 끝나면 utter.onend에서 STT 자동 시작
                        speak("매수를 선택해주세요. 몇 명이 예매할지 먼저 정해야 합니다.");
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
        if (recognition) {
            try {
                recognition.stop();
                console.log("🎤 기존 음성 인식 중단 요청");
            } catch (e) {
                console.warn("STT 중단 중 오류:", e);
            }
        }

        // stop() 완료될 시간을 조금 기다렸다가 다음 안내 실행
        setTimeout(() => {
            // 안내 (이때는 autoListen = false 로 지정)
            speak(`🎟️ ${ticketCount}매 자동 예매를 진행합니다.`, false);

            // 안내가 끝난 뒤 약간 쉬었다가 STT 재시작
            setTimeout(() => {
                try {
                    recognition.start();
                    console.log("🎤 자동예매 안내 후 STT 재시작됨");
                } catch (err) {
                    console.error("자동예매 단계 재시작 오류:", err);
                }
            }, 1200);
        }, 500); // stop() 후 안정적으로 종료될 시간 확보
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
                        <div style={{ textAlign: "center", lineHeight: "1.6" }}>
                            👥 <strong style={{ color: "#CA2E26" }}>매수를 선택해주세요.</strong>
                            <br />
                            몇 명이 예매할지 먼저 정해야 합니다.
                        </div>
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
                        <div style={{ textAlign: "center", lineHeight: "1.6" }}>
                            🎟️ <strong style={{ color: "#CA2E26" }}>이 버튼을 눌러</strong>
                            <br />
                            자동 예매를 진행해보세요!
                        </div>
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
