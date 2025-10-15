import React, { useEffect, useState } from "react";
import { AppBar, Toolbar, Box, Typography, Button } from "@mui/material";

const Header = () => {
  // [*] 메시지를 저장할 상태
  const [ message , setMessage ] = useState([]);
  // [*] 웹소켓 객체 상태 저장
  const [ ws , setWs ] = useState(null);
  // [1] 컴포넌트가 마운트될때 한번 실행
  useEffect( () => {
    // 웹소켓 연결 생성
    const socket = new WebSocket("http://localhost:8080/socket");
    // 상태에 객체 저장
    setWs(socket);

    // [1-1] 웹소켓 열렸을때 실행
    socket.onopen = () => {
      console.log('WebSocket 연결성공');
    }// func end
    
    // [1-2] 서버로부터 메시지 받았을때 실행
    socket.onmessage = (event) => {
      // 수신 데이터는 문자열이므로 JSON으로 파싱
      const data = JSON.parse(event.data);
      console.log('수신메시지',data);
      // 기존 메시지 상태에 새로운 메시지 추가
      setMessage( (prev) => [...prev,data]);
    }// func end

    // [1-3] 웹소켓 에러 발생 시 실행
    socket.onerror = (e) => {
      console.log('WebSocket 에러' , e);
    }// func end

    // [1-4] 웹소켓 연결 종료 시 실행
    socket.onclose = () => {
      console.log('WebSocket 연결종료');
    }// func end

    // [1-5] 컴포넌트 언마운트시 웹소켓 연결종료
    return () => {
      socket.close();
    }// func end    
  } , [] );
  
  // [2] 메시지 전송함수 
  const sendMessage = (msg) => {
    // 웹소켓이 연결되어 있고 열려있을때만 전송
    if(ws && ws.readyState === WebSocket.OPEN){
      ws.send(JSON.stringify(msg));
    }// if end
  }// func end



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
