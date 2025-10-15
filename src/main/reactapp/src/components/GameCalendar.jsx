import React from "react";
import { Box, Typography, Avatar, IconButton } from "@mui/material";
import { ChevronLeft, ChevronRight } from "@mui/icons-material";
import dayjs from "dayjs";
import "./GameCalendar.css";

const GameCalendar = ({ year, month, matches, onDateClick, onMonthChange }) => {
  const start = dayjs(`${year}-${month}-01`);
  const daysInMonth = start.daysInMonth();
  const firstDay = start.day();

  const days = [];
  for (let i = 0; i < firstDay; i++) days.push(null);
  for (let d = 1; d <= daysInMonth; d++) days.push(d);

  return (
    <Box className="calendar-wrapper">
      {/* 월 변경 헤더 */}
      <Box className="calendar-header">
        <IconButton onClick={() => onMonthChange(-1)} size="small" sx={{ color: "#CA2E26" }}>
          <ChevronLeft />
        </IconButton>
        <Typography variant="h6" fontWeight="bold" color="#CA2E26">
          {year}년 {month}월
        </Typography>
        <IconButton onClick={() => onMonthChange(1)} size="small" sx={{ color: "#CA2E26" }}>
          <ChevronRight />
        </IconButton>
      </Box>

      {/* 요일 */}
      <div className="calendar-weekdays">
        {["일", "월", "화", "수", "목", "금", "토"].map((day) => (
          <div key={day} className="calendar-weekday">
            {day}
          </div>
        ))}
      </div>

      {/* 날짜 칸 */}
      <div className="calendar-grid">
        {days.map((day, i) => {
          const dateStr =
            day &&
            `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
          const match = matches.find((m) => m.date === dateStr);

          return (
            <div
              key={i}
              className="calendar-cell"
              onClick={() => day && onDateClick(dateStr)}
            >
              {day && (
                <>
                  <Typography className="calendar-day-number">{day}</Typography>
                  {match && (
                    <div className="calendar-match">
                      <Avatar src={match.logoA} className="calendar-logo" />
                      <Typography className="calendar-score">{match.score}</Typography>
                      <Avatar src={match.logoB} className="calendar-logo" />
                      <Typography className="calendar-time">{match.time}</Typography>
                    </div>
                  )}
                </>
              )}
            </div>
          );
        })}
      </div>
    </Box>
  );
};

export default GameCalendar;
