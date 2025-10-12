import React from "react";
import { Box, Typography } from "@mui/material";

const Footer = () => {
  return (
    <Box
      sx={{
        bgcolor: "#111",
        color: "#aaa",
        textAlign: "center",
        py: 3,
        mt: "auto",
      }}
    >
      <Typography variant="body2" sx={{ opacity: 0.8 }}>
        © 2025 Incheon Phoenix Baseball. All Rights Reserved.
      </Typography>
      <Typography variant="caption" sx={{ opacity: 0.6 }}>
        본 서비스는 학습 및 포트폴리오용으로 제작되었습니다.
      </Typography>
    </Box>
  );
};

export default Footer;
