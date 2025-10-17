import React, { useState } from "react";
import { Box, Typography, Divider } from "@mui/material";
import dayjs from "dayjs";
import GameCalendar from "../components/GameCalendar";
import "../styles/Home.css";

const matches = [
  {
    date: "2025-09-02",
    teamA: "Eagles",
    teamB: "Phoenix",
    score: "3 : 2",
    time: "18:30",
    stadium: "대전구장",
    logoA: "/assets/eagles.png",
    logoB: "/assets/phoenix.png",
  },
  {
    date: "2025-09-05",
    teamA: "Landers",
    teamB: "Phoenix",
    score: "8 : 4",
    time: "17:00",
    stadium: "창원문학",
    logoA: "/assets/landers.png",
    logoB: "/assets/phoenix.png",
  },
  {
    date: "2025-09-12",
    teamA: "Phoenix",
    teamB: "Bears",
    score: "4 : 5",
    time: "18:30",
    stadium: "광주야구장",
    logoA: "/assets/phoenix.png",
    logoB: "/assets/bears.png",
  },
];

const Home = () => {
  const today = dayjs();
  const [year, setYear] = useState(today.year());
  const [month, setMonth] = useState(today.month() + 1);
  const [selectedDate, setSelectedDate] = useState(null);

  const handleMonthChange = (delta) => {
    const newMonth = month + delta;
    if (newMonth === 0) {
      setMonth(12);
      setYear((y) => y - 1);
    } else if (newMonth === 13) {
      setMonth(1);
      setYear((y) => y + 1);
    } else {
      setMonth(newMonth);
    }
    setSelectedDate(null);
  };

  const selectedMatch = matches.find((m) => m.date === selectedDate);

  return (
    <Box className="home-container">
      <Typography variant="h4" className="home-title">
        경기 일정 / 결과
      </Typography>

      {/* ✅ 달력 위에 경기 정보 섹션 추가 */}
      <Box className="match-detail-section">
        {selectedMatch ? (
          <>
            <Typography variant="h6" fontWeight="bold" color="#CA2E26">
              {selectedMatch.teamA} vs {selectedMatch.teamB}
            </Typography>
            <Typography variant="body1" sx={{ mt: 1 }}>
              점수: {selectedMatch.score}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              {selectedMatch.time} | {selectedMatch.stadium}
            </Typography>
          </>
        ) : (
          <Typography color="text.secondary" fontSize="0.9rem">
            날짜를 클릭하면 해당 경기 정보가 표시됩니다.
          </Typography>
        )}
      </Box>

      {/* ⚾ 달력 */}
      <GameCalendar
        year={year}
        month={month}
        matches={matches}
        onDateClick={setSelectedDate}
        onMonthChange={handleMonthChange}
      />

      <Divider sx={{ my: 4 }} />

      {/* (추가 콘텐츠 영역 자리) */}
      <Box className="below-section">
        <Typography color="text.secondary" textAlign="center">
          — 다른 콘텐츠나 경기 통계가 여기에 들어갑니다 —
        </Typography>
      </Box>
    </Box>
  );
};

export default Home;
