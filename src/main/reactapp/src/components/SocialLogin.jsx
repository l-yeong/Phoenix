import React from "react";
import { Button } from "@mui/material";
import styles from "../styles/Auth.module.css";

const SocialLogin = () => {

    const handleSocialLogin = (provider) => {
        window.location.href = `http://192.168.40.190:8080/oauth2/authorization/${provider}`;
    };

    return(
        <div className={styles.socialBox}>
            <Button
                variant="outlined"
                onClick={() => handleSocialLogin("google")}
                sx={{color : "#DB4437" , borderColor : "#DB4437" }}
            >
                Google 로그인
            </Button>

            <Button
                variant="outlined"
                onClick={() => handleSocialLogin("github")}
                sx={{ color: "#24292f", borderColor: "#24292f" }}
            >
                GitHub 로그인
            </Button>

            <Button
                variant="outlined"
                onClick={() => handleSocialLogin("facebook")}
                sx={{ color: "#1877f2", borderColor: "#1877f2" }}
            >
                Facebook 로그인
            </Button>
        </div>
    );
};

export default SocialLogin;