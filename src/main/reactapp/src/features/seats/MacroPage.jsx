// src/pages/MacroPage.jsx
import React, { useEffect, useState, useCallback, useMemo } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../../styles/macro.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function MacroPage() {
  const navigate = useNavigate();
  const { state } = useLocation();

  const gno =
    Number(state?.gno) ||
    Number(new URLSearchParams(window.location.search).get("gno")) ||
    Number(sessionStorage.getItem("gate_gno")) ||
    0;

  const [captchaImg, setCaptchaImg] = useState("");
  const [captchaToken, setCaptchaToken] = useState("");
  const [answer, setAnswer] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);

  // ⬇️ 토스트 상태/헬퍼 (alert 대체)
  const [toast, setToast] = useState({ open: false, msg: "", type: "error" });
  const toastTimer = useMemo(() => ({ id: null }), []);
  const showToast = useCallback((msg, type = "error", ms = 2200) => {
    if (toastTimer.id) clearTimeout(toastTimer.id);
    setToast({ open: true, msg, type });
    toastTimer.id = setTimeout(() => setToast(t => ({ ...t, open: false })), ms);
  }, [toastTimer]);

  const loadCaptcha = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API}/captcha/new`, { credentials: "include" });
      const data = await res.json();
      setCaptchaImg(data.imageBase64);
      setCaptchaToken(data.token);
    } catch {
      showToast("⚠️ 캡차를 불러오지 못했습니다. 다시 시도해 주세요.");
    } finally {
      setLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (!gno) {
      navigate("/gate", { replace: true });
      return;
    }
    sessionStorage.setItem("gate_gno", String(gno));
    loadCaptcha();
  }, [gno, loadCaptcha, navigate]);

  useEffect(() => {
    if (!gno) return;
    const handleUnload = () => {
      try {
        const url = `${API}/gate/leave?gno=${encodeURIComponent(gno)}`;
        const blob = new Blob([], { type: "text/plain" });
        navigator.sendBeacon(url, blob);
      } catch {}
    };
    window.addEventListener("beforeunload", handleUnload);
    return () => window.removeEventListener("beforeunload", handleUnload);
  }, [gno]);

  const handleVerify = async () => {
    if (!captchaToken) {
      showToast("캡차가 준비되지 않았습니다. 새로고침 후 다시 시도해 주세요.");
      return;
    }
    if (!answer.trim()) {
      showToast("이미지의 문자를 입력해 주세요.");
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API}/captcha/verify`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ token: captchaToken, answer }),
      });

      const raw = await res.text();
      let data = {};
      try { data = raw ? JSON.parse(raw) : {}; } catch {}

      if (res.ok && data?.ok) {
        // 성공 토스트는 짧게 보여주고 이동
        showToast("✅ 인증 완료! 좌석 페이지로 이동합니다.", "success", 1200);
        navigate("/seats", { replace: true, state: { gno } });
      } else {
        showToast(
          data?.message ||
            (res.status === 400
              ? "❌ 문자가 일치하지 않습니다. 다시 시도해 주세요."
              : "🚨 검증 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.")
        );
        loadCaptcha();
        setAnswer("");
      }
    } catch {
      showToast("🚨 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter") handleVerify();
  };

  return (
    <div className="macro-page">
      <h2 className="macro-title">🤖 매크로 인증</h2>
      <div className="macro-meta">
        경기 번호: <b>{gno || "-"}</b>
      </div>

      <div className="macro-card">
        <div className="macro-captcha">
          {captchaImg ? (
            <img src={captchaImg} alt="captcha" />
          ) : (
            <div className="macro-loading">로딩 중...</div>
          )}
        </div>

        <div className="macro-row">
          <input
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="이미지의 문자를 입력하세요"
            className="macro-input"
            autoFocus
          />
          <button
            onClick={loadCaptcha}
            disabled={loading}
            className="btn btn-ghost"
            type="button"
            title="새 이미지로 교체"
          >
            {loading ? "새로고침…" : "새로고침"}
          </button>
        </div>

        <button
          onClick={handleVerify}
          disabled={!answer || submitting}
          className="btn btn-primary btn-full"
          type="button"
        >
          {submitting ? "확인 중..." : "입력 완료"}
        </button>
      </div>

      <p className="macro-note">
        캡차를 통과하면 좌석 선택 화면으로 이동합니다. <br />
        새 창을 닫거나 새로고침하면 대기열에서 자동 퇴장 처리됩니다.
      </p>

      {/* ⬇️ 토스트 컴포넌트 */}
      <div
        className={[
          "toast-wrap",
          toast.open ? "toast-show" : "toast-hide",
          toast.type === "success" ? "toast-success" : "toast-error",
        ].join(" ")}
        role="status"
        aria-live="polite"
      >
        {toast.msg}
      </div>
    </div>
  );
}
