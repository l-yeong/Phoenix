// src/pages/Home.jsx
import React, { useEffect, useMemo, useState } from "react";
import { Box, Typography, Divider, Button } from "@mui/material";
import dayjs from "dayjs";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import GameCalendar from "../components/GameCalendar";
import "../styles/Home.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const DEFAULT_STADIUM = "홈구장";

export default function Home() {
  const navigate = useNavigate();

  const today = dayjs();
  const [year, setYear] = useState(today.year());
  const [month, setMonth] = useState(today.month() + 1);
  const [selectedDate, setSelectedDate] = useState(null);
  const [matches, setMatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      try {
        const { data } = await axios.get(`${API}/game/all`, { withCredentials: true });
        if (!mounted) return;
        const mapped = (data || []).map((g) => ({
          gno: Number(g.gno),
          date: g.date,
          time: g.time,
          teamA: g.homeTeam,
          teamB: g.awayTeam,
          score: g.score,
          result: g.result,
          stadium: g.stadium || DEFAULT_STADIUM,
          reservable: g.reservable,
        }));
        setMatches(mapped);
      } catch (e) {
        console.error(e);
        setError("경기 데이터를 불러오지 못했습니다.");
      } finally {
        setLoading(false);
      }
    })();
    return () => (mounted = false);
  }, []);

  const monthMatches = useMemo(() => {
    return matches.filter((m) => {
      const d = dayjs(m.date, "YYYY-MM-DD");
      return d.year() === year && d.month() + 1 === month;
    });
  }, [matches, year, month]);

  const selectedMatch = matches.find((m) => m.date === selectedDate) || null;

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

  const handleReserveClick = (gno) => {
    navigate("/gate", { state: { gno } });
  };

  return (
    <Box className="home-container">
      <Typography variant="h4" className="home-title">경기 일정 / 결과</Typography>
      {loading && <Typography>불러오는 중...</Typography>}
      {error && <Typography color="error">{error}</Typography>}

      <Box className="match-detail-section">
        {selectedMatch ? (
          <>
            <Typography variant="h6" fontWeight="bold" color="#CA2E26">
              {selectedMatch.teamA} vs {selectedMatch.teamB}
            </Typography>
            <Typography variant="body1" sx={{ mt: 1 }}>
              {selectedMatch.time} | {selectedMatch.stadium}
            </Typography>

            {selectedMatch.reservable ? (
              <Button
                variant="contained"
                sx={{ mt: 2, backgroundColor: "#CA2E26" }}
                onClick={() => handleReserveClick(selectedMatch.gno)}
              >
                예매하기
              </Button>
            ) : (
              <Typography sx={{ mt: 2, color: "gray" }}>
                예매 불가 (기간 외 or 종료)
              </Typography>
            )}
          </>
        ) : (
          <Typography color="text.secondary" fontSize="0.9rem">
            날짜를 클릭하면 해당 경기 정보가 표시됩니다.
          </Typography>
        )}
      </Box>

      <GameCalendar
        year={year}
        month={month}
        matches={monthMatches}
        onDateClick={setSelectedDate}
        onMonthChange={handleMonthChange}
      />

      <Divider sx={{ my: 4 }} />
      <Typography textAlign="center" color="text.secondary">— 추가 콘텐츠 자리 —</Typography>
    </Box>
  );
}
