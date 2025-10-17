import React from "react";
import { Button, TextField, Typography } from "@mui/material";
import styles from "../styles/Auth.module.css";
import api from "../api/axiosInstance";
import { Password } from "@mui/icons-material";


const SignUpForm = () => {
    const [ form ,  setForm ] = React.useState({
        mid : "",
        Password_hash : "",
        mname : "",
        email : "",
        birthdate : "",
    });

    const handleChange = (e) => {
        setForm({ ...form , [e.target.name] : e.target.value})
    };

    const handleSignUp = async (e) => {
        e.preventDefault();
        try{
            const response = await api.post("/members/signup" , form);
            if( response.status === 200 ){
                alert("회원가입 성공");
                window.location.href = "/login";
            }

        }catch(error){
            console.error(error);
            alert("회원가입 실패");
        }
    };

    return(
        <div className={styles.container}>
            <form className={styles.form} onSubmit={handleSignUp}>
                <Typography variant="h5" className={styles.title}>
                    🧾 회원가입
                </Typography>    
                {[
                    { label : "아이디" , name : "mid" },
                    { label : "비밀번호" , name : "password_hash" , type : "password" },
                    { label : "이름" , name : "mname" },
                    { label : "이메일" , name : "email" },
                    { label : "생년월일" , name : "birthdate" , type : "date" },

                ].map((field) => (
                    <TextField
                        key = {field.name}
                        label = {field.label}
                        name = {field.name}
                        type= {field.type || "text"}
                        fullWidth
                        className={styles.input}
                        value={form[field.name]}
                        onChange={handleChange}
                        InputLabelProps = {
                            field.type === "date" ? { shrink : true } : undefined
                        }
                    />
                ))}

                <Button type="submit" variant="containde" className={styles.button}>
                    회원가입
                </Button>
            
            </form>    
        </div>
    );

};

export default SignUpForm;