import React, { useEffect, useState, useCallback, useMemo, useRef } from "react";
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

  // JWT
  const authHeaders = useMemo(() => {
    const t = localStorage.getItem("jwt");
    return t ? { Authorization: `Bearer ${t}` } : {};
  }, []);

  const [captchaImg, setCaptchaImg] = useState("");
  const [captchaToken, setCaptchaToken] = useState("");
  const [answer, setAnswer] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(false);

  const [ttlSec, setTtlSec] = useState(null);
  const [ready, setReady] = useState(false);

  // 좌석으로 이동할 때만 게이트 유지
  const keepGateRef = useRef(false);

  // 토스트
  const [toast, setToast] = useState({ open: false, msg: "", type: "error" });
  const toastTimer = useRef(null);
  const showToast = useCallback((msg, type = "error", ms = 2200) => {
    clearTimeout(toastTimer.current);
    setToast({ open: true, msg, type });
    toastTimer.current = setTimeout(
      () => setToast((t) => ({ ...t, open: false })),
      ms
    );
  }, []);

  const fmt = (s) =>
    s == null ? "--:--" : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

  // 게이트 체크 1회
  const checkGate = useCallback(async () => {
    if (!gno) return { ok: false, ttl: 0 };
    try {
      const res = await fetch(`${API}/gate/check/${encodeURIComponent(gno)}`, {
        credentials: "include",
        headers: { ...authHeaders },
      });
      if (res.status === 401) {
        showToast("로그인이 만료되었습니다. 다시 로그인해 주세요.", "error");
        navigate("/home", { replace: true });
        return { ok: false, ttl: 0 };
      }
      const data = await res.json();
      const ok = !!data?.ready;
      const ttl = Number(data?.ttlSec ?? 0);
      setReady(ok);
      setTtlSec(ttl > 0 ? ttl : 0);
      return { ok, ttl: ttl > 0 ? ttl : 0 };
    } catch {
      return { ok: false, ttl: 0 };
    }
  }, [gno, authHeaders, navigate, showToast]);

  // TTL 카운트다운
  useEffect(() => {
    if (ttlSec == null) return;
    const t = setInterval(() => setTtlSec((v) => (v == null ? v : Math.max(0, v - 1))), 1000);
    return () => clearInterval(t);
  }, [ttlSec]);

  useEffect(() => {
    if (ready && ttlSec === 0) {
      sessionStorage.removeItem("gate_gno");
      navigate(`/gate?expired=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
    }
  }, [ready, ttlSec, gno, navigate]);

  // 캡차 로드 (★ gno는 쿼리스트링으로!)
  const loadCaptcha = useCallback(async () => {
    if (!gno) return;
    setLoading(true);
    try {
      const res = await fetch(`${API}/captcha/new?gno=${encodeURIComponent(gno)}`, {
        credentials: "include",
        headers: { ...authHeaders },
      });
      if (res.status === 401) {
        // 게이트 미입장/만료 or 로그인 만료
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
        return;
      }
      if (!res.ok) {
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
        return;
      }
      const data = await res.json();
      setCaptchaImg(data.imageBase64);
      setCaptchaToken(data.token);
    } catch {
      showToast("⚠️ 캡차를 불러오지 못했습니다. 다시 시도해 주세요.");
    } finally {
      setLoading(false);
    }
  }, [gno, authHeaders, navigate, showToast]);

  // 최초 진입
  useEffect(() => {
    if (!gno) { navigate("/gate", { replace: true }); return; }
    sessionStorage.setItem("gate_gno", String(gno));

    (async () => {
      const { ok, ttl } = await checkGate();
      if (!ok || ttl <= 0) {
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
        return;
      }
      await loadCaptcha();
    })();

    // 새로고침/닫기 시 leave
    const onUnload = () => {
      try {
        fetch(`${API}/gate/leave?gno=${encodeURIComponent(gno)}`, {
          method: "POST",
          credentials: "include",
          keepalive: true,
          headers: { "Content-Type": "application/json", ...authHeaders },
          body: JSON.stringify(gno),
        });
      } catch {}
    };
    window.addEventListener("beforeunload", onUnload);
    window.addEventListener("pagehide", onUnload);

    // 언마운트: 좌석으로 가는 경우만 게이트 유지
    return () => {
      window.removeEventListener("beforeunload", onUnload);
      window.removeEventListener("pagehide", onUnload);
      if (!keepGateRef.current && gno) onUnload();
    };
  }, [gno, authHeaders, checkGate, loadCaptcha, navigate]);

  // 검증 (★ gno는 쿼리스트링으로!)
  const handleVerify = async () => {
    if (!captchaToken) { showToast("캡차가 준비되지 않았습니다. 새로고침 후 다시 시도해 주세요."); return; }
    if (!answer.trim()) { showToast("이미지의 문자를 입력해 주세요."); return; }

    setSubmitting(true);
    try {
      const res = await fetch(`${API}/captcha/verify?gno=${encodeURIComponent(gno)}`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", ...authHeaders },
        body: JSON.stringify({ token: captchaToken, answer }),
      });

      if (res.status === 401) {
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
        return;
      }

      const data = await res.json(); // { ok: -1|0|1 }
      const result = data?.ok;

      if (res.ok) {
        if (result === 1) {
          keepGateRef.current = true; // 좌석으로 이동 → 게이트 유지
          showToast("✅ 인증 완료! 좌석 페이지로 이동합니다.", "success", 900);
          setTimeout(() => navigate("/seats", { replace: true, state: { gno } }), 250);
        } else if (result === 0) {
          showToast("❌ 문자가 일치하지 않습니다. 다시 시도해 주세요.");
          setAnswer("");
          await loadCaptcha();
        } else if (result === -1) {
          showToast("⚠️ 인증 시간이 만료되었습니다. 새 캡차를 불러옵니다.");
          setAnswer("");
          await loadCaptcha();
        } else {
          showToast("🚨 예기치 않은 응답입니다. 다시 시도해 주세요.");
          setAnswer("");
          await loadCaptcha();
        }
      } else {
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
      }
    } catch {
      showToast("🚨 네트워크 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleKeyDown = (e) => { if (e.key === "Enter") handleVerify(); };

  return (
    <div className="macro-page">
      <h2 className="macro-title">🤖 매크로 인증</h2>
      <div className="macro-meta">
        경기 번호: <b>{gno || "-"}</b> · 게이트 <b>{fmt(ttlSec)}</b>
      </div>

      <div className="macro-card">
        <div className="macro-captcha">
          {captchaImg ? <img src={captchaImg} alt="captcha" /> : <div className="macro-loading">로딩 중...</div>}
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
