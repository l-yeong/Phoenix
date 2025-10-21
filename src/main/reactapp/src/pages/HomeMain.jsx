import { Box, Grid, Typography, Button } from "@mui/material";
import { useNavigate } from "react-router-dom";
import styles from "../styles/HomeMain.module.css";

const HomeMain = () => {
  const navigate = useNavigate();

  return (
    <Box className={styles.container}>
      {/* 로고 + 타이틀 */}
      <Box className={styles.logoSection}>
        <img
          src="/images/phoenix_logo_red.png"
          alt="Phoenix Logo"
          className={styles.logo}
        />
        <Typography variant="h2" className={styles.title}>
          PHOENIX 야구 예매 서비스
        </Typography>
        <Typography variant="h6" className={styles.subtitle}>
          원하시는 예매 방식을 선택하세요.
        </Typography>
      </Box>

      {/* 3가지 예매 선택 */}
      <Grid container spacing={5} justifyContent="center" className={styles.menuSection}>
        {/* 일반 예매 */}
        <Grid item xs={12} sm={6} md={4}>
          <Box className={styles.card}>
            <Typography variant="h4" className={styles.cardTitle}>
              일반 예매
            </Typography>
            <Typography className={styles.cardDesc}>
              직접 경기 일정을 선택하고 좌석을 예매합니다.
            </Typography>
            <Button
              className={styles.cardButton}
              onClick={() => navigate("/home")}
            >
              바로가기 →
            </Button>
          </Box>
        </Grid>

        {/* 시니어 예매 */}
        <Grid item xs={12} md={3.5}>
          <Box className={styles.card}>
            <Typography variant="h4" className={styles.cardTitle}>
              시니어 예매
            </Typography>
            <Typography className={styles.cardDesc}>
              노년층 전용 음성 안내 및 간편 예매 서비스입니다.
            </Typography>
            <Button
              className={styles.cardButton}
              onClick={() => navigate("/senior-reserve")}
            >
              바로가기 →
            </Button>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default HomeMain;
