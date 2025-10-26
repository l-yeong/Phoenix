import React, { useState, useEffect } from "react";
import {
  Box,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
} from "@mui/material";
import { useSearchParams } from "react-router-dom";
import styles from "../styles/SeniorReserve.module.css";
import TutorialOverlay from "../components/TutorialOverlay";

export default function SeniorSeatAuto() {
  const [searchParams] = useSearchParams();
  const gameId = searchParams.get("gameId");
  const [ticketCount, setTicketCount] = useState(1);

  // 튜토리얼 단계 관리
  const [guideStep, setGuideStep] = useState(0); // 0: 비활성, 1: 매수선택, 2: 자동예매버튼

  useEffect(() => {
    const timer = setTimeout(() => setGuideStep(1), 400); // 1단계부터 시작
    return () => clearTimeout(timer);
  }, []);

  const handleAutoReserve = () => {
    alert("시니어 자동 예매 기능은 곧 추가될 예정입니다.");
    // 추후 자동예매 API 연동 (/reserve/auto)
  };

  return (
    <Box className={styles.container}>
      <Typography variant="h5" className={styles.title}>
        🎟️ 시니어 자동 좌석 배정
      </Typography>

      <Typography variant="subtitle1" className={styles.subtitle}>
        경기 ID: {gameId} <br />
        매수를 선택하고 자동예매를 진행하세요.
      </Typography>

      {/* 매수 선택 Select Box */}
      <FormControl fullWidth style={{ marginTop: "30px" }}>
        <InputLabel id="ticket-count-label">매수 선택</InputLabel>
        <Select
          id="ticketSelectBox" // 튜토리얼 타겟 ID
          labelId="ticket-count-label"
          value={ticketCount}
          onChange={(e) => setTicketCount(e.target.value)}
        >
          <MenuItem value={1}>1매</MenuItem>
          <MenuItem value={2}>2매</MenuItem>
        </Select>
      </FormControl>

      {/* 자동 예매 버튼 */}
      <Button
        id="autoReserveButton" // 튜토리얼 타겟 ID
        variant="contained"
        color="error"
        className={styles.reserveButton}
        onClick={handleAutoReserve}
      >
        ⚾ 자동 예매하기
      </Button>

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
          onClose={() => setGuideStep(2)} // 다음 단계로 이동
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
          onClose={() => setGuideStep(0)} // 종료
        />
      )}
    </Box>
  );
}
