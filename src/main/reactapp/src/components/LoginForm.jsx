import { Button, TextField, Typography } from "@mui/material";
import { useState } from "react";
import styles from "../styles/Auth.module.css";
import api from "../api/axiosInstance";
import SocialLogin from "./SocialLogin";
import { useAuth } from "../api/loginstate.jsx";
import { Navigate, useNavigate } from "react-router-dom";

const LoginForm = () => {

    const navigate = useNavigate();
    const { login } = useAuth();
    const [mid, setMid] = useState("");
    const [password, setPassword] = useState("");

    const handleLogin = async (e) => {
        e.preventDefault();
        try {
            const response = await api.post("/members/login", {
                mid,
                password_hash: password,
            });

            console.log("백엔드 응답:", response.data);

            // 백엔드 구조: { success, message, data: { accessToken, mid, mno } }
            const resData = response.data.data || response.data;

            if (!resData) {
                alert("서버 응답이 올바르지 않습니다.");
                return;
            }


            // 서버 응답에서 토큰 , mno , mid 꺼내기
            const token = resData.accessToken; // 백엔드 JWT 토큰 문자열 반환
            const mno = resData.mno;
            const memberId = resData.mid;

            console.log(" 로그인 직전 token:", token);
            console.log(" 로그인 직전 mid:", memberId);
            console.log(" 로그인 직전 mno:", mno);

            if (token) {

                login({ token, mid: memberId, mno }); // 전역 상태 + localStorage 동시 저장
                alert("로그인 성공!");
                navigate("/");
            } else {
                alert("토큰이 없습니다.");
            }

        } catch (error) {
            console.error("로그인 요청 중 오류:", error);

            if (error.response) {
                console.log("서버 응답 코드:", error.response.status);
                console.log("서버 응답 데이터:", error.response.data);
            } else if (error.request) {
                console.log("요청은 보냈지만 응답이 없습니다:", error.request);
            } else {
                console.log("요청 설정 중 오류:", error.message);
            }

            alert("로그인 실패");
        }
    };

    return (
        <div className={styles.container}>
            <form className={styles.form} onSubmit={handleLogin}>
                <Typography variant="h5" className={styles.title}>
                    🔥 Phoenix 로그인
                </Typography>

                <TextField
                    label="아이디"
                    fullWidth
                    className={styles.input}
                    value={mid}
                    onChange={(e) => setMid(e.target.value)}
                />

                <TextField
                    label="비밀번호"
                    type="password"
                    fullWidth
                    className={styles.input}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                />

                <Button
                    variant="contained"
                    className={styles.Button}
                    onClick={(e) => {
                        e.preventDefault();                 // 기본 동작 방지
                        console.log(" handleLogin 실행됨");  // 클릭 확인용 로그
                        handleLogin(e);                     // 로그인 함수 직접 실행
                    }}
                >
                    로그인
                </Button>

                <SocialLogin />

            </form>

        </div>
    );
};

export default LoginForm;