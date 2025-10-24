// ✅ src/components/Header.jsx
import axios from "axios";
import React, { useEffect, useState, useRef } from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import { ToastContainer, toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import styles from "../styles/Header.module.css";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../api/loginstate.jsx";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const Header = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const wsRef = useRef(null);
  const [message, setMessage] = useState([]);

  /**
   * ✅ WebSocket 연결 (로그인 상태에서만)
   */
  useEffect(() => {
    if (!user) return;
    if (wsRef.current) return;

    const socket = new WebSocket("ws://localhost:8080/socket");
    wsRef.current = socket;

    socket.onopen = () => {
      console.log("[Header] WebSocket 연결성공");
      socket.send(JSON.stringify({ type: "login", mno: user.mno }));
    };

    socket.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        setMessage((prev) => [...prev, data]);
        toast.info(`${typeof data === "string" ? data : data.message}`, {
          position: "bottom-right",
          autoClose: 5000,
          theme: "colored",
        });
      } catch (e) {
        console.log("수신 파싱 오류", e);
      }
    };

    socket.onerror = (e) => console.log("[Header] WebSocket 에러", e);
    socket.onclose = () => {
      console.log("[Header] WebSocket 연결종료");
      wsRef.current = null;
    };

    return () => {
      try {
        socket.close();
      } catch {}
      wsRef.current = null;
    };
  }, [user]);

    const onLogout = async () => {
    console.log("[Header] 🚪 로그아웃 시작");

    const gno = Number(sessionStorage.getItem("gate_gno"));
    if (gno) {
      try {
        const res = await axios.post(`${API}/gate/leave`, gno, {
          withCredentials: true,
          headers: { "Content-Type": "application/json" },
        });
        console.log("[Header] ✅ gate/leave 성공:", res.data);
      } catch (err) {
        console.error("[Header] ❌ gate/leave 실패:", err);
        alert(`[Header] gate/leave 실패: ${err.response?.status || err.message}`);
      }
    }

    try {
      wsRef.current?.close();
    } catch {}
    wsRef.current = null;

    localStorage.removeItem("jwt");
    sessionStorage.removeItem("gate_gno");

    await logout?.();

    toast.success("로그아웃 되었습니다.", { autoClose: 1000 });

    setTimeout(() => {
      console.log("[Header] 🔁 로그아웃 완료 → /login 이동");
      navigate("/login", { replace: true });
    }, 400);
  };

  return (
    <AppBar position="relative" color="transparent" className={styles.appBar}>
      <Toolbar className={styles.toolbar}>
        <Typography
          variant="h6"
          color="inherit"
          className={styles.logo}
          onClick={() => navigate("/")}
        >
          ⚾ PHOENIX
        </Typography>

        <Box className={styles.nav}>
          {["TICKET", "PLAYERS", "GAME", "CONTENTS", "MEMBERSHIP"].map((menu) => (
            <Button key={menu} className={styles.navButton}>
              {menu}
            </Button>
          ))}
        </Box>

        <Box className={styles.auth}>
          {user && user?.role !== "ROLE_WITHDRAWN" ? (
            <>
              <Typography
                variant="body1"
                sx={{ color: "white", marginRight: "20px", fontWeight: "500" }}
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
                onClick={onLogout}
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

      <ToastContainer
        position="bottom-right"
        autoClose={4000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnHover
        theme="colored"
      />
    </AppBar>
  );
};

export default Header;
