import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../api/loginstate";

const SocialSuccess = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  // facebook 리다이렉트 URL '#_=_' 버그 제거해서 프론트에서 제대로 읽게 함
  useEffect(() => {
    // location.hash 값이 존재하고 , 그 값이 '#_=_' 인지 검사
    if (window.location.hash && window.location.hash === "#_=_") {
      // 브라우저가 history API를 지원하면
      // 주소창에 '#_=_' 제거 하되 페이지 새로고침 없이 처리
      history.replaceState
        ? history.replaceState(null, null, window.location.href.split("#"[0]))
        // history API 없으면 그냥 hash 값을 빈 문자열로 덮어쓰기
        : (window.location.hash = "")
    }
  }, []);

  useEffect(() => {
    const token = params.get("token");
    const mid = params.get("mid");
    const mno = params.get("mno");

    if (localStorage.getItem("accessToken")) {
      console.log("이미 로그인 처리 완료");
      return;
    }

    if (token) {
      // context , localstorage 동시 갱신
      login({ token, mid, mno });
      alert(`${mid || "회원"}님 , 로그인 성공!`);
      //  3. 홈으로 이동
      navigate("/");
    } else {
      alert("토큰이 존재하지 않습니다. 로그인 실패");
      navigate("/login");
    }
  }, [navigate, params, login]);

  return (
    <div style={{ textAlign: "center", marginTop: "100px" }}>
      <h2>소셜 로그인 처리중...</h2>
    </div>
  );
};

export default SocialSuccess;
