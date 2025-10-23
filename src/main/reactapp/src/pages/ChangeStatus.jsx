import React from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { Box, Typography, Button } from "@mui/material";
import api from "../api/axiosInstance";

/**
 * ReactivatePage.jsx
 * ---------------------------------------------------
 * 휴면(탈퇴) 계정 복구 페이지
 * - 회원 상태가 'withdrawn'일 때 접근
 * - 복구 버튼 클릭 시 /members/reactivate API 호출
 * - 복구 완료 후 로그인 페이지로 이동 (/login)
 */
const ChangeStatus = () => {
  const [params] = useSearchParams();
  const mid = params.get("mid"); // 회원 아이디
  const navigate = useNavigate();

  /** 복구 처리 요청 */
  const handleReactivate = async () => {
    try {
      const res = await api.post("/members/changestatus", { mid });

      if (res.data.success) {
        alert(res.data.message || "계정이 복구되었습니다. 다시 로그인해주세요.");
        navigate("/login"); // 복구 완료 후 로그인 페이지로 이동
      } else {
        alert(res.data.message || "복구에 실패했습니다.");
      }
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || "서버 오류로 복구 요청에 실패했습니다.");
    }
  };

  return (
    <Box
      sx={{
        textAlign: "center",
        mt: 15,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
      }}
    >
      <Typography
        variant="h5"
        sx={{ mb: 2, color: "#CA2E26", fontWeight: "bold" }}
      >
         휴면 계정 안내
      </Typography>

      <Typography variant="body1" sx={{ mb: 4 }}>
        회원님의 계정은 현재 <b>휴면 상태</b>입니다.
        <br />
        계정을 복구하고 다시 이용하시겠습니까?
      </Typography>

      <Button
        variant="contained"
        onClick={handleReactivate}
        sx={{
          bgcolor: "#CA2E26",
          color: "white",
          fontWeight: "bold",
          width: "200px",
          "&:hover": { bgcolor: "#b22720" },
        }}
      >
        계정 복구하기
      </Button>
    </Box>
  );
};

export default ChangeStatus;
