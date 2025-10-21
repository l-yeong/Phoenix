import React, { useEffect, useState , useRef } from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";
import { ToastContainer , toast } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import styles from "../styles/Header.module.css";
import { useNavigate } from "react-router-dom";
import {useAuth} from "../api/loginstate.jsx";

const Header = () => {

  const navigate = useNavigate();
  const { user, logout } = useAuth(); // 로그인 상태 전역 접근
  const wsRef = useRef(null);

  // [*] 메시지를 저장할 상태
  const [ message , setMessage ] = useState([]);
  // [*] 웹소켓 객체 상태 저장
  const [ ws , setWs ] = useState(null);
  // [1] 컴포넌트가 마운트될때 한번 실행
  useEffect( () => {
    if (!user) return; // 로그인 안됐으면 리턴
    if (!wsRef.current){ // 이미 열려있으면 재생성 X
      // 웹소켓 연결 생성
      const socket = new WebSocket("ws://localhost:8080/socket");
      // 상태에 객체 저장
      setWs(socket);

      // [1-1] 웹소켓 열렸을때 실행
      socket.onopen = () => {
        console.log('WebSocket 연결성공');
        socket.send(JSON.stringify({ type: "login", mno: user.mno }));
        console.log("회원번호 자동전송",user.mno);
      }// func end

      // [1-2] 서버로부터 메시지 받았을때 실행
      socket.onmessage = (event) => {
        // 수신 데이터는 문자열이므로 JSON으로 파싱
        const data = JSON.parse(event.data);
        console.log('수신메시지',data);
        // 기존 메시지 상태에 새로운 메시지 추가
        setMessage( (prev) => [...prev,data]);
        toast.info(` ${typeof data === "string" ? data : data.message}`,{
          position: "bottom-right",
          autoClose: 5000,
          theme: "colored",
        });
      }// func end

      // [1-3] 웹소켓 에러 발생 시 실행
      socket.onerror = (e) => {
        console.log('WebSocket 에러' , e);
      }// func end

      // [1-4] 웹소켓 연결 종료 시 실행
      socket.onclose = () => {
        console.log('WebSocket 연결종료');
      }// func end

    }// if end
  } , [user] );
  const socketClose = () => {
    logout();
    if(wsRef.current && wsRef.current.readyState === WebSocket.OPEN){
      wsRef.current.close();
      wsRef.current = null;
      console.log("소켓 종료")
    }// if end
  }// func end

  // 현재 로그인 상태 콘솔로 확인
  console.log(" Header 렌더링됨, 현재 user:", user);
  console.log("현재 user 상태:", user);

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
                onClick={socketClose}
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
        {/* 토스트 컨테이너 추가 */}
      <ToastContainer
        position="bottom-right"
        autoClose={5000}
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        pauseOnHover
        theme="colored"
      />
    </AppBar>
  )
}

export default Header;
