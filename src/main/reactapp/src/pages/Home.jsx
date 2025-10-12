import React from "react";
import { Box, Typography } from "@mui/material";

const Home = () => {
  return (
    <Box sx={{ textAlign: "center", mt: 4 }}>
      <Typography variant="h4" fontWeight="bold" color="#CA2E26">
        PHOENIX 경기 일정 / 결과
      </Typography>
      <Typography variant="body1" sx={{ mt: 2, color: "#555" }}>
        ⚾ 여기에 메인 콘텐츠(달력/경기 일정) 표시될 예정
      </Typography>
    </Box>
  );
};

export default Home;
