import { Button, TextField, Typography } from "@mui/material";
import { useState } from "react";
import styles from "../styles/Auth.module.css";
import api from "../api/axiosInstance";
import SocialLogin from "./SocialLogin";

const LoginForm = () => {

    const [ mid , setMid ] = useState("");
    const [ password , setPassword ] = useState("");

    const handleLogin = async (e) => {
        e.preventDefault();
        try{
            const response = await api.post("/members/login" ,{
                mid ,
                password_hash : password,
            });

            const token = response.data // 백엔드 JWT 토큰 문자열 반환
            localStorage.setItem("accceessToken" , token);
            alert("로그인 성공");
            window.location.href = "/";

        }catch(error){
            console.error(error);
            alert("로그인 실패");
        }
    };

    return (
        <div className={styles.container}>
            <form className = {styles.form} onSubmit={handleLogin}>
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

                <Button type="submit" variant="contained" className={styles.Button}>
                    로그인
                </Button>

                <SocialLogin />

            </form>

        </div>
        );
};

export default LoginForm;