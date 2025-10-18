import React from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import styles from "../styles/Header.module.css";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../api/loginstate.jsx";

const Header = () => {

  const navigate = useNavigate();
  const { user, logout } = useAuth(); // 로그인 상태 전역 접근


  // 현재 로그인 상태 콘솔로 확인
  console.log(" Header 렌더링됨, 현재 user:", user);
  console.log("현재 user 상태:", user);

  return (
    <AppBar position="relative" color="transaparent" className={styles.appBar}>
      <Toolbar className={styles.toolbar}>
        {/* 로고 */}
        <Typography variant="h6" color="inherit" className={styles.logo}
          onClick= {() => navigate("/")}
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
