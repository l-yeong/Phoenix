import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../api/loginstate"; // AuthContext 훅

/**
 * 소셜 로그인 성공 처리 페이지
 * - OAuth2SuccessHandler 리다이렉트 URL에서 전달된 mid/mno 파라미터 처리
 * - JWT는 서버에서 쿠키로 이미 저장됨
 * - 클라이언트에서는 추가로 localStorage 저장 불필요
 */
const SocialSuccess = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  /**
   * 페이스북 '#_=_' 버그 제거
   */
  useEffect(() => {
    if (window.location.hash && window.location.hash === "#_=_") {
      history.replaceState
        ? history.replaceState(null, null, window.location.href.split("#")[0])
        : (window.location.hash = "");
    }
  }, []);

  /**
   * 리다이렉트 시 전달된 회원 정보 처리
   * - 백엔드에서 JWT 쿠키 저장 후 mid, mno만 쿼리스트링으로 전달됨
   */
  useEffect(() => {
    const mid = params.get("mid");
    const mno = params.get("mno");

    if (mid && mno) {
      // 토큰은 쿠키에 있으므로 별도 저장 불필요
      login({ mid, mno });
      alert(`${mid}님, 로그인 성공!`);
      navigate("/");
    } else {
      alert("로그인 정보가 올바르지 않습니다. 다시 시도해주세요.");
      navigate("/login");
    }
  }, []);

  return (
    <div style={{ textAlign: "center", marginTop: "100px" }}>
      <h2>소셜 로그인 처리중...</h2>
    </div>
  );
};

export default SocialSuccess;
