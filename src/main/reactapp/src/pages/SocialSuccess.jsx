import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

const SocialSuccess = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = params.get("token");
    if (token) {
      //  1. 토큰 저장
      localStorage.setItem("accessToken", token);

      //  2. 안내 메시지 (선택)
      alert("소셜 로그인 성공!");

      //  3. 홈으로 이동
      navigate("/");
    } else {
      alert("토큰이 존재하지 않습니다. 로그인 실패");
      navigate("/login");
    }
  }, [navigate, params]);

  return (
    <div style={{ textAlign: "center", marginTop: "100px" }}>
      <h2>소셜 로그인 처리중...</h2>
    </div>
  );
};

export default SocialSuccess;
