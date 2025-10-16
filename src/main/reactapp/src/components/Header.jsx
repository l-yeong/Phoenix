import React from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import styles from "./Header.module.css";

const Header = () =>  {
  return(
    <AppBar position="static" className={styles.appBar}>
      <Toolbar className={styles.toolbar}>
        {/* 로고 */}
        <Typography variant="h6" className={styles.logo}>
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
              <Button variant="outlined" className={styles.loginBtn}>
                로그인
              </Button>
              
              <Button variant="contained" className={styles.signupBtn}>
                회원가입
              </Button>

            </Box>

      </Toolbar>
    </AppBar>
  )
}

export default Header;
