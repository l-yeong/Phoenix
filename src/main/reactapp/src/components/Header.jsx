import React from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";

const Header = () => {
  return (
    <AppBar
      position="static"
      sx={{
        bgcolor: "#CA2E26",
        height: "70px",
        justifyContent: "center",
      }}
    >
      <Toolbar
        sx={{
          width: "1280px",
          mx: "auto",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
        }}
      >
        {/* 로고 */}
        <Typography
          variant="h6"
          sx={{ fontWeight: "bold", cursor: "pointer" }}
        >
          ⚾ PHOENIX
        </Typography>

        {/* 네비게이션 */}
        <Box sx={{ display: "flex", gap: 4 }}>
          {["TICKETS", "PLAYERS", "GAME", "CONTENTS", "MEMBERSHIP"].map(
            (menu) => (
              <Button
                key={menu}
                sx={{
                  color: "white",
                  fontWeight: "bold",
                  "&:hover": { opacity: 0.8 },
                }}
              >
                {menu}
              </Button>
            )
          )}
        </Box>

        {/* 로그인 */}
        <Box sx={{ display: "flex", gap: 1 }}>
          <Button
            variant="outlined"
            sx={{
              color: "white",
              borderColor: "white",
              "&:hover": { bgcolor: "rgba(255,255,255,0.2)" },
            }}
          >
            로그인
          </Button>
          <Button
            variant="contained"
            sx={{
              bgcolor: "white",
              color: "#CA2E26",
              fontWeight: "bold",
              "&:hover": { bgcolor: "#f8f8f8" },
            }}
          >
            회원가입
          </Button>
        </Box>
      </Toolbar>
    </AppBar>
  );
};

export default Header;
