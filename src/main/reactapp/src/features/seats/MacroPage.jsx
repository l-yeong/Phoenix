// src/pages/gate/MacroPage.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

/**
 * [MacroPage]
 * - Gate 통과 후 진입하는 "인증(캡차)" 페이지
 * - GatePage에서 { state: { mno, gno } } 로 전달받음 (정수)
 * - 새로고침 시 sessionStorage(gate_mno/gate_gno)로 복구
 * - 게이트 ready 안 된 채로 접근하면 /gate로 자동 복귀
 * - 캡차 검증 성공 → /seats 로 이동
 */
export default function MacroPage() {
  const navigate = useNavigate();
  const { state } = useLocation();

  // ✅ mno/gno 로 받기 (state 우선, 없으면 sessionStorage fallback)
  const [mno, setMno] = useState(() => {
    if (typeof state?.mno === "number") return state.mno;
    const saved = sessionStorage.getItem("gate_mno");
    return saved ? Number(saved) : undefined;
  });
  const [gno, setGno] = useState(() => {
    if (typeof state?.gno === "number") return state.gno;
    const saved = sessionStorage.getItem("gate_gno");
    return saved ? Number(saved) : undefined;
  });

  // ✅ 캡차 상태
  const [captchaImg, setCaptchaImg] = useState("");
  const [captchaToken, setCaptchaToken] = useState("");
  const [answer, setAnswer] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // 세션스토리지 동기화(새로고침 복구용)
  useEffect(() => {
    if (mno) sessionStorage.setItem("gate_mno", String(mno));
    if (gno) sessionStorage.setItem("gate_gno", String(gno));
  }, [mno, gno]);

  // ✅ 게이트 ready 확인: ready 아니면 /gate로 자동 복귀, ready면 머무르며 캡차 진행
  useEffect(() => {
    if (!mno || !gno) {
      navigate("/gate", { replace: true });
      return;
    }

    let stop = false;
    const tick = async () => {
      try {
        const res = await fetch(`${API_BASE}/gate/check/${encodeURIComponent(mno)}`);
        const data = await res.json();
        if (!data?.ready) {
          // 아직 게이트 입장 전이면 게이트로 되돌린다.
          navigate("/gate", { replace: true });
        }
        // ready면 그대로 이 페이지에서 캡차 진행
      } catch (_) {
        // 네트워크 일시 오류는 무시하고 재시도
      }
    };

    // 최초 한 번 즉시 확인 + 1초 후 재확인(짧게)
    tick();
    const t = setTimeout(() => !stop && tick(), 1000);
    return () => {
      stop = true;
      clearTimeout(t);
    };
  }, [mno, gno, navigate]);

  // ✅ 캡차 새로 받기
  const loadCaptcha = useMemo(
    () => async () => {
      setError("");
      setAnswer("");
      try {
        const res = await fetch(`${API_BASE}/captcha/new`, { method: "GET" });
        const data = await res.json();
        setCaptchaImg(data.imageBase64);
        setCaptchaToken(data.token);
      } catch {
        setError("캡차 이미지를 불러오지 못했습니다. 네트워크 상태를 확인해주세요.");
      }
    },
    []
  );

  // 최초 로딩 시 캡차 발급
  useEffect(() => {
    loadCaptcha();
  }, [loadCaptcha]);

  // ✅ 캡차 검증 → 성공 시 좌석 페이지로 이동
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
        setError("문자가 일치하지 않습니다. 다시 시도해주세요.");
        await loadCaptcha(); // 오답/실패 시 새 캡차 발행
        return;
      }

      // ✅ 성공 → /seats 이동 (뒤로가기 방지)
      navigate("/seats", {
        replace: true,
        state: { mno, gno, captchaOk: true },
      });
    } catch {
      setError("검증 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="max-w-sm mx-auto p-6">
      <div className="text-xs inline-flex items-center gap-1 px-2 py-1 rounded-full bg-blue-50 text-blue-700 border border-blue-200 mb-3">
        <span aria-hidden>✔</span> 안심예매
      </div>

      <h2 className="text-2xl font-bold mb-1">문자를 입력해주세요</h2>
      <p className="text-sm text-gray-600 mb-4">
        부정 예매 방지를 위해 아래의 문자를 입력해주세요. 인증 후 좌석 선택이 가능합니다.
      </p>

      <div className="border rounded-md p-3 bg-white">
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

          {/* 새로고침 버튼 */}
          <button
            type="button"
            onClick={loadCaptcha}
            title="새로고침"
            className="absolute right-2 top-2 bg-white/80 border rounded-full w-8 h-8 grid place-items-center"
          >
            ↻
          </button>
        </div>

        <input
          value={answer}
          onChange={(e) => setAnswer(e.target.value)}
          placeholder="이미지의 문자를 입력해주세요"
          className="border rounded px-3 py-2 w-full"
          aria-label="캡차 정답 입력"
          onKeyDown={(e) => {
            if (e.key === "Enter") handleVerify(); // 엔터로 제출
          }}
        />
      </div>

      {error && <div className="text-sm text-red-600 mt-2">{error}</div>}

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

      <div className="text-xs text-gray-500 mt-3 leading-5">
        • 입력값이 틀릴 경우 새 캡차가 자동으로 발급됩니다. <br />
        • 캡차는 기본적인 자동화 방지용이며, 상용 솔루션 도입을 권장합니다.
      </div>
    </div>
  );
}
