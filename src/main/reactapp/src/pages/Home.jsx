// src/pages/Home.jsx
// -------------------------------------------------------------
// 경기 달력 + “예매하기”까지 연결된 화면
// - 최초 마운트 시 백엔드 /game/all 로 전체 경기 목록 로드
// - 달력에서 날짜를 클릭하면 상단 요약박스에 해당 경기 표시
// - “예매하기” 클릭 시 프론트에서 예매 가능 조건(7일 규칙/종료 여부) 사전검증
//   → 대기열 등록(/gate/enqueue) → /gate 페이지로 이동
// -------------------------------------------------------------

import React, { useEffect, useMemo, useState } from "react";
import { Box, Typography, Divider, Button } from "@mui/material";
import dayjs from "dayjs";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import GameCalendar from "../components/GameCalendar";
import "../styles/Home.css";

// ✅ 백엔드 베이스 URL (환경변수 없으면 로컬)
const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// 점수 포맷: "3대4" → "3 : 4"
const prettyScore = (raw) =>
  !raw || raw.trim() === "" ? "" : raw.replace("대", " : ");

// 단일구장이면 달력 카드에서 보여줄 기본 구장명
const DEFAULT_STADIUM = "홈구장";

export default function Home() {
  const navigate = useNavigate();
  const today = dayjs();

  // ✅ 월/연도/선택일 (기존 로직 유지)
  const [year, setYear] = useState(today.year());
  const [month, setMonth] = useState(today.month() + 1);
  const [selectedDate, setSelectedDate] = useState(null);

  // ✅ 서버에서 가져온 전체 경기 목록
  //  - GameCalendar가 사용하는 필드(teamA/teamB/date/time/score/stadium)는 유지
  //  - gno/원본 점수/결과 등 내부용 데이터는 언더스코어로 보관
  const [matches, setMatches] = useState([]);

  // UI 보조 상태
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // ✅ 데모용 사용자 번호 (실제로는 로그인 완료 시 세션/스토리지에서 가져와 사용)
  const MNO =
    Number(sessionStorage.getItem("mno")) || // 로그인 시 저장해뒀다면 사용
    20001; // 없으면 데모 기본값

  // -------------------------------------------------------------
  // 1) 마운트 시 전체 경기 로드
  // -------------------------------------------------------------
  useEffect(() => {
    let mounted = true;
    (async () => {
      setLoading(true);
      setError(null);
      try {
        // 백엔드: GameController @GetMapping("/game/all")
        const { data } = await axios.get(`${API}/game/all`);
        const mapped = (data || []).map((g) => ({
          // UI에서 쓰는 값
          gno: Number(g.gno),
          date: g.date, // "YYYY-MM-DD"
          time: g.time, // "HH:mm"
          teamA: g.homeTeam,
          teamB: g.awayTeam,
          score: prettyScore(g.score || ""), // ""면 달력에 점수 미표시
          stadium: DEFAULT_STADIUM,

          // 내부용 값(예매 가능 판단 등에 사용)
          _result: g.result, // null/"" 이면 미종료
          _rawScore: g.score, // null/"" 이면 미종료
          _homePitcher: g.homePitcher,
          _awayPitcher: g.awayPitcher,
        }));
        if (mounted) setMatches(mapped);
      } catch (e) {
        console.error(e);
        if (mounted) setError("경기 데이터를 불러오지 못했습니다.");
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  // -------------------------------------------------------------
  // 2) 월 전환 (기존 로직 유지)
  // -------------------------------------------------------------
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

  // -------------------------------------------------------------
  // 3) 현재 페이지(연/월)에 해당하는 경기만 필터링 (달력이 월 필터를 안한다면)
  // -------------------------------------------------------------
  const monthMatches = useMemo(() => {
    return matches.filter((m) => {
      const d = dayjs(m.date, "YYYY-MM-DD");
      return d.year() === year && d.month() + 1 === month;
    });
  }, [matches, year, month]);

  // 상단 요약 카드에 표시할 선택 경기
  const selectedMatch = matches.find((m) => m.date === selectedDate) || null;

  // -------------------------------------------------------------
  // 4) “예매하기” 버튼 핸들러
  //    - 프론트 사전 검증(7일 전 ~ 경기 시작 직전 && 미종료)
  //    - 대기열 등록(/gate/enqueue) → /gate로 이동
  // -------------------------------------------------------------
  const handleReserveClick = async (match) => {
    try {
      // (1) 프론트 사전 검증
      const gameDT = dayjs(`${match.date} ${match.time}`, "YYYY-MM-DD HH:mm");
      const now = dayjs();

      const within7days =
        now.isAfter(gameDT.subtract(7, "day")) && now.isBefore(gameDT);
      const notEnded = !match._result && !match._rawScore;

      if (!within7days || !notEnded) {
        alert("예매 불가 경기입니다. (기간이 아니거나 이미 종료된 경기)");
        return;
      }

      // (2) 대기열 등록 요청 (백엔드 GateController @PostMapping("/gate/enqueue"))
      const payload = { mno: Number(MNO), gno: Number(match.gno) };
      const { data } = await axios.post(`${API}/gate/enqueue`, payload);

      if (!data?.queued) {
        alert("이미 예매했거나 등록할 수 없습니다.");
        return;
      }

      // (3) 게이트 페이지에서 읽을 값 저장
      sessionStorage.setItem("gate_mno", String(MNO));
      sessionStorage.setItem("gate_gno", String(match.gno));
      sessionStorage.setItem("gate_queued", "1");

      alert("입장 대기열 등록 완료! 곧 입장 가능합니다.");

      // (4) ✅ 게이트 페이지로 이동 → GatePage에서 폴링하면서 자동 진입
      navigate("/gate", { state: { mno: MNO, gno: Number(match.gno) } });
    } catch (e) {
      console.error(e);
      alert("대기열 등록 중 오류가 발생했습니다.");
    }
  };

  // -------------------------------------------------------------
  // 5) 렌더
  // -------------------------------------------------------------
  return (
    <Box className="home-container">
      <Typography variant="h4" className="home-title">
        경기 일정 / 결과
      </Typography>

      {/* 로딩/에러 상태 표시 */}
      {loading && (
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          불러오는 중…
        </Typography>
      )}
      {error && (
        <Typography color="error" sx={{ mt: 1 }}>
          {error}
        </Typography>
      )}

      {/* ✅ 달력 위 요약 카드 (선택한 날짜가 있을 때 표시) */}
      <Box className="match-detail-section">
        {selectedMatch ? (
          <>
            <Typography variant="h6" fontWeight="bold" color="#CA2E26">
              {selectedMatch.teamA} vs {selectedMatch.teamB}
            </Typography>
            <Typography variant="body1" sx={{ mt: 1 }}>
              {selectedMatch.score ? `점수: ${selectedMatch.score}` : "점수: -"}
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              {selectedMatch.time} | {selectedMatch.stadium}
            </Typography>

            {/* ✅ 예매하기 버튼: 프론트 사전 검증 → 대기열 등록 → /gate 이동 */}
            <Button
              variant="contained"
              sx={{ mt: 2, backgroundColor: "#CA2E26" }}
              onClick={() => handleReserveClick(selectedMatch)}
            >
              예매하기
            </Button>
          </>
        ) : (
          <Typography color="text.secondary" fontSize="0.9rem">
            날짜를 클릭하면 해당 경기 정보가 표시됩니다.
          </Typography>
        )}
      </Box>

      {/* ⚾ 달력 (월 전환/일자클릭 콜백은 기존과 동일) */}
      <GameCalendar
        year={year}
        month={month}
        matches={monthMatches}
        onDateClick={setSelectedDate}
        onMonthChange={handleMonthChange}
      />

      <Divider sx={{ my: 4 }} />

      {/* (추가 콘텐츠 자리) */}
      <Box className="below-section">
        <Typography color="text.secondary" textAlign="center">
          — 다른 콘텐츠나 경기 통계가 여기에 들어갑니다 —
        </Typography>
      </Box>
    </Box>
  );
}
