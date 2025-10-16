import React from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import styles from "../styles/Header.module.css";
import { useNavigate } from "react-router-dom";

const Header = () => {

  const navigate = useNavigate();

  return (
    <AppBar position="relative" className={styles.appBar}>
      <Toolbar className={styles.toolbar}>
        {/* 로고 */}
        <Typography variant="h6" className={styles.logo}
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

        {/* 로그인/회원가입 버튼 */}
        <Box className={styles.auth}>
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

        </Box>

      </Toolbar>
    </AppBar>
  )
}

export default Header;
