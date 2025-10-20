import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import styles from "../styles/Header.module.css";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../api/loginstate.jsx";
import React, { useRef, useEffect } from "react";

const Header = () => {

  const navigate = useNavigate();
  const { user, logout } = useAuth(); // 로그인 상태 전역 접근
  const loggedOnce = useRef(false); //  로그 출력 여부 저장

  // 로그인 상태 로그 1회만 출력
  useEffect(() => {
    if (!loggedOnce.current) {
      console.log("[Header] 현재 로그인 상태:", user);
      loggedOnce.current = true; // 이후엔 로그 안 찍힘
    }
  }, [user]);

  return (
    <AppBar position="relative" color="transparent" className={styles.appBar}>
      <Toolbar className={styles.toolbar}>
        {/* 로고 */}
        <Typography variant="h6" color="inherit" className={styles.logo}
          onClick={() => navigate("/")}
        >
          ⚾ PHOENIX
        </Typography>
        {/* 네비게이션 메뉴 */}
        <Box className={styles.nav}>
          {["TICKET", "PLAYERS", "GAME", "CONTENTS", "MEMBERSHIP"].map(
            (menu) => (
              <Button key={menu} className={styles.navButton}>
                {menu}
              </Button>
            )
          )}
        </Box>

        {/* 로그인 상태에 따른 UI 분기 */}
        <Box className={styles.auth}>
          {user ? (
            <>
              <Typography
                variant="body1"
                sx={{
                  color: "white",
                  marginRight: "20px",
                  fontWeight: "500",
                }}
              >
                {user.mid}님
              </Typography>
              <Button
                variant="outlined"
                color="inherit"
                onClick={() => navigate("/mypage")}
                sx={{ marginRight: "10px" }}
              >
                마이페이지
              </Button>
              <Button
                variant="contained"
                color="secondary"
                onClick={logout}
                sx={{
                  backgroundColor: "#fff",
                  color: "#CA2E26",
                  fontWeight: "bold",
                }}
              >
                로그아웃
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="outlined"
                className={styles.loginBtn}
                onClick={() => navigate("/login")}
              >
                로그인
              </Button>
              <Button
                variant="contained"
                className={styles.signupBtn}
                onClick={() => navigate("/signup")}
              >
                회원가입
              </Button>
            </>
          )}
        </Box>

      </Toolbar>
    </AppBar>
  )
}

export default Header;
