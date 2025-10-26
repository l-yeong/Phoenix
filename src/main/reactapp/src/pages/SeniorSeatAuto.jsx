import React, { useState } from "react";
import { Box, Typography, FormControl, InputLabel, Select, MenuItem, Button } from "@mui/material";
import { useSearchParams } from "react-router-dom";
import styles from "../styles/SeniorReserve.module.css";

export default function SeniorSeatAuto() {
  const [searchParams] = useSearchParams();
  const gameId = searchParams.get("gameId");
  const [ticketCount, setTicketCount] = useState(1);

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

      <FormControl fullWidth style={{ marginTop: "30px" }}>
        <InputLabel id="ticket-count-label">매수 선택</InputLabel>
        <Select
          labelId="ticket-count-label"
          value={ticketCount}
          onChange={(e) => setTicketCount(e.target.value)}
        >
          <MenuItem value={1}>1매</MenuItem>
          <MenuItem value={2}>2매</MenuItem>
        </Select>
      </FormControl>

      <Button
        variant="contained"
        color="error"
        className={styles.reserveButton}
        onClick={handleAutoReserve}
      >
        ⚾ 자동 예매하기
      </Button>
    </Box>
  );
}
