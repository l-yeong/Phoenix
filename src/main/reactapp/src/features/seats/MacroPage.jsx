import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

/**
 * MacroPage (내부 문자 캡차)
 *
 * 역할
 *  - GatePage에서 넘겨준 userId/showId를 바탕으로
 *  - 백엔드(/captcha/new)에서 캡차 이미지를 받아 표시하고
 *  - 사용자가 입력한 값을 /captcha/verify로 검증한 뒤
 *  - 성공 시 /seats 페이지로 이동한다.
 *
 * 전제
 *  - 백엔드에 아래 2개 엔드포인트가 있어야 한다.
 *    GET  /captcha/new      -> { token, imageBase64 }
 *    POST /captcha/verify   -> body: { token, answer }  resp: { ok: boolean }
 *
 * 보안 메모
 *  - 이 방식은 “내부 캡차”로서 기본적인 억제용이다.
 *  - 고난도 봇/공격에는 상용 솔루션(reCAPTCHA/hCaptcha/Turnstile) 검토 권장.
 */

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function MacroPage() {
  const navigate = useNavigate();

  // GatePage에서 navigate로 넘겨준 state를 받는다.
  //  - state: { userId, showId }
  const { state } = useLocation();
  const userId = state?.userId;
  const showId = state?.showId;

  // 캡차 이미지/토큰/입력값/에러/로딩 상태
  const [captchaImg, setCaptchaImg] = useState("");
  const [captchaToken, setCaptchaToken] = useState("");
  const [answer, setAnswer] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 0) 진입 가드: Gate를 안 거쳤거나 파라미터가 없으면 /gate로 되돌린다.
  useEffect(() => {
    if (!userId || !showId) navigate("/gate");
  }, [userId, showId, navigate]);

  // 1) 캡차 새로 불러오기
  const loadCaptcha = async () => {
    setError("");
    setAnswer("");
    try {
      const res = await fetch(`${API_BASE}/captcha/new`);
      const data = await res.json();
      // 서버가 내려준 base64 이미지와 토큰을 상태로 저장
      setCaptchaImg(data.imageBase64);
      setCaptchaToken(data.token);
    } catch (e) {
      setError("캡차 이미지를 불러오지 못했습니다. 네트워크 상태를 확인해주세요.");
    }
  };

  // 최초 진입 시 1회 캡차 로드
  useEffect(() => {
    loadCaptcha();
  }, []);

  // 2) 검증 버튼 클릭 -> /captcha/verify 호출
  const handleVerify = async () => {
    if (!captchaToken || !answer || submitting) return;
    setSubmitting(true);
    setError("");

    try {
      const res = await fetch(`${API_BASE}/captcha/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token: captchaToken, answer }),
      });
      const data = await res.json();

      if (!data?.ok) {
        // 실패: 에러 메시지 노출 + 새 캡차 로드
        setError("문자가 일치하지 않습니다. 다시 시도해주세요.");
        await loadCaptcha();
        return;
      }

      // 성공: 좌석 페이지로 이동
      //  - token은 “캡차 통과했다” 정도의 플래그로만 사용 (서버 검증을 더 강화하려면 서버에서 별도 nonce를 내려주는 방식을 권장)
      navigate("/seats", { state: { userId, showId, token: "local-captcha-ok" } });
    } catch (e) {
      setError("검증 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-sm mx-auto p-6">
      {/* 상단 배지/타이틀/설명 */}
      <div className="text-xs inline-flex items-center gap-1 px-2 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-200 mb-3">
        <span aria-hidden>✔</span> 안심예매
      </div>
      <h2 className="text-2xl font-bold mb-1">문자를 입력해주세요</h2>
      <p className="text-sm text-gray-600 mb-4">
        부정 예매 방지를 위해 아래의 문자를 입력해주세요. 인증 후 좌석을 선택할 수 있습니다.
      </p>

      {/* 캡차 카드 */}
      <div className="border rounded-md p-3 bg-white">
        {/* 이미지 영역 + 새로고침 버튼 */}
        <div className="relative flex items-center justify-center mb-2 min-h-[84px]">
          {captchaImg ? (
            <img
              src={captchaImg}
              alt="캡차 이미지"
              className="select-none max-w-full"
              draggable={false}
            />
          ) : (
            <div className="text-sm text-gray-500">로딩 중…</div>
          )}

        {/* 새로고침 버튼: 새 캡차 요청 */}
          <button
            type="button"
            onClick={loadCaptcha}
            title="새로고침"
            className="absolute right-2 top-2 bg-white/80 border rounded-full w-8 h-8 grid place-items-center"
          >
            ↻
          </button>

          {/* 접근성/확장: 오디오 캡차 버튼 자리는 나중에 붙일 수 있음 */}
          {/* <button aria-label="오디오 재생" className="absolute right-12 top-2 ...">🔊</button> */}
        </div>

        {/* 입력창 */}
        <input
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          placeholder="이미지의 문자를 입력해주세요"
          className="border rounded px-3 py-2 w-full"
          aria-label="캡차 정답 입력"
        />
      </div>

      {/* 오류 메시지 */}
      {error && <div className="text-sm text-red-600 mt-2">{error}</div>}

      {/* 액션: ‘입력완료’만 남김(요청대로 날짜 선택 제거) */}
      <div className="mt-4 flex gap-2">
        <button
          type="button"
          onClick={handleVerify}
          disabled={!captchaToken || !answer || submitting}
          className={`px-4 py-2 rounded text-white ${
            !captchaToken || !answer || submitting ? "bg-gray-400" : "bg-gray-800"
          }`}
        >
          {submitting ? "확인 중..." : "입력완료"}
        </button>
      </div>

      {/* 안내 문구 */}
      <div className="text-xs text-gray-500 mt-3 leading-5">
        • 입력값이 틀릴 경우 새 캡차가 자동으로 발급됩니다. <br />
        • 캡차는 보안 억제용이며, 대규모 공격 대응은 상용 솔루션 도입을 권장합니다.
      </div>
    </div>
  );
}
