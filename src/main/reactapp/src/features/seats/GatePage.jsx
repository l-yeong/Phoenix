// src/pages/GatePage.jsx
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import "../../styles/gate.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function GatePage() {
  const navigate = useNavigate();
  const location = useLocation();

  const api = useMemo(
    () => axios.create({ baseURL: API, withCredentials: true }),
    []
  );

  const gno =
    Number(location.state?.gno) ||
    Number(new URLSearchParams(window.location.search).get("gno")) ||
    0;

  const [queued, setQueued] = useState(false);
  const [waitingCount, setWaitingCount] = useState(0);   // ← 현재 대기 인원(실시간)
  const [position, setPosition] = useState(-1);
  const [ready, setReady] = useState(false);
  const [ttlSec, setTtlSec] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const pollTimerRef = useRef(null);
  const tickTimerRef = useRef(null);

  // 매크로로 이동할 땐 언마운트 leave 금지
  const goingMacroRef = useRef(false);

  // 앞에 남은 인원
  const [ahead, setAhead] = useState(0);

  const fmt = (s) =>
    s == null ? "--:--" : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;

  // JWT
  const authHeaders = useMemo(() => {
    const t = localStorage.getItem("jwt");
    return t ? { Authorization: `Bearer ${t}` } : {};
  }, []);

  // beforeunload/pagehide 때만 leave (새로고침/닫기)
  useEffect(() => {
    if (!gno) return;
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
    return () => {
      window.removeEventListener("beforeunload", onUnload);
      window.removeEventListener("pagehide", onUnload);
    };
  }, [gno, authHeaders]);

  // 다른 경로로 떠날 때만 leave (매크로 이동 제외)
  useEffect(() => {
    return () => {
      clearTimeout(pollTimerRef.current);
      clearInterval(tickTimerRef.current);
      if (!goingMacroRef.current && gno) {
        fetch(`${API}/gate/leave?gno=${encodeURIComponent(gno)}`, {
          method: "POST",
          credentials: "include",
          keepalive: true,
          headers: { "Content-Type": "application/json", ...authHeaders },
          body: JSON.stringify(gno),
        }).catch(() => {});
      }
    };
  }, [gno, authHeaders]);

  // 토스트
  const [toast, setToast] = useState({ open: false, msg: "", type: "info" });
  const toastTimer = useRef(null);
  const showToast = useCallback((msg, type = "info", ms = 2200) => {
    clearTimeout(toastTimer.current);
    setToast({ open: true, msg, type });
    toastTimer.current = setTimeout(
      () => setToast((t) => ({ ...t, open: false })),
      ms
    );
  }, []);

  // ?expired=1 | ?requeue=1 안내
  useEffect(() => {
    const p = new URLSearchParams(window.location.search);
    if (p.get("expired") === "1") showToast("세션이 만료되어 대기열에 재등록합니다.", "warn", 2200);
    if (p.get("requeue") === "1") showToast("다시 대기열에 등록합니다.", "info", 1800);
  }, [showToast]);

  // 대기열 등록
  const enqueue = useCallback(async () => {
    if (!gno) return;
    setLoading(true);
    setError("");
    try {
      const { data } = await api.post("/gate/enqueue", gno, {
        headers: { "Content-Type": "application/json", ...authHeaders },
      });

      if (data.waiting === -1 || data.msg === "이미 예매 완료된 사용자입니다.") {
        showToast("이미 예매한 경기입니다. 마이페이지에서 확인해 주세요.", "warn", 2600);
setTimeout(() => navigate("/home", { replace: true }), 2200);
        return;
      }
      if (!data.queued) {
        showToast("대기열 등록 실패 — 예약이 불가능합니다.", "error");
        navigate("/home", { replace: true });
        return;
      }

      setQueued(true);
      setWaitingCount(Number(data?.waiting ?? 0)); // 최초 값
      setMessage("안정적인 운영을 위해 대기 순서대로 입장합니다.");
    } catch {
      setError("로그인이 필요합니다.");
      showToast("로그인 후 이용해 주세요.", "error");
      navigate("/home", { replace: true });
    } finally {
      setLoading(false);
    }
  }, [api, gno, authHeaders, navigate, showToast]);

  // 최초 진입: 바로 enqueue
  useEffect(() => {
    if (!gno) { navigate("/home", { replace: true }); return; }
    enqueue();
  }, [gno, enqueue, navigate]);

  // 폴링: check & position  ← 여기서 대기열/앞사람 수 실시간 업데이트
  useEffect(() => {
    if (!queued || !gno) return;

    let fail = 0;
    const tick = async () => {
      try {
        const [{ data: check }, { data: pos }] = await Promise.all([
          api.get(`/gate/check/${gno}`, { headers: { ...authHeaders } }),
          api.get(`/gate/position/${gno}`, { headers: { ...authHeaders } }),
        ]);

        setReady(!!check?.ready);
        setTtlSec(Number(check?.ttlSec ?? 0));

        // 현재 대기열(큐) 길이도 매 틱 갱신
        setWaitingCount(Number(check?.waiting ?? 0));

        const p = typeof pos?.position === "number" ? pos.position : -1;
        setPosition(p);
        setAhead(p > 0 ? p - 1 : 0);

        fail = 0;
        pollTimerRef.current = setTimeout(tick, 1000);
      } catch {
        fail = Math.min(fail + 1, 6);
        pollTimerRef.current = setTimeout(tick, 800 + 200 * fail);
      }
    };

    tick();
    tickTimerRef.current = setInterval(() => {
      setTtlSec((v) => (v == null ? v : Math.max(0, v - 1)));
    }, 1000);

    return () => {
      clearTimeout(pollTimerRef.current);
      clearInterval(tickTimerRef.current);
    };
  }, [queued, gno, api, authHeaders]);

  // ready → 매크로 이동 (leave 금지 플래그)
  useEffect(() => {
    if (!ready) return;
    goingMacroRef.current = true;
    sessionStorage.setItem("gate_gno", String(gno));
    navigate("/macro", { replace: true, state: { gno } });
  }, [ready, gno, navigate]);

  const progress = useMemo(() => {
    if (position === 0) return 100;
    if (waitingCount <= 0 || position < 0) return 20;
    const p = Math.round(((waitingCount - position) / Math.max(waitingCount, 1)) * 100);
    return Math.min(95, Math.max(5, p));
  }, [waitingCount, position]);

  const cancelQueue = async () => {
    try {
      await api.post(`/gate/leave?gno=${encodeURIComponent(gno)}`, null, {
        headers: { ...authHeaders },
      });
    } catch {}
    navigate("/home", { replace: true });
  };

  return (
    <div className="gate">
      <div className="gate__inner">
        <header className="gate__header">
          <div className="brand">
            <span className="brand__dot" />
            <span className="brand__name">Phoenix Ticket</span>
          </div>
          {ttlSec != null && ttlSec > 0 && (
            <span className={`ttl ${ttlSec <= 30 ? "ttl--warn" : ""}`}>게이트 {fmt(ttlSec)}</span>
          )}
        </header>

        <main className="gate__card">
          <h1 className="card__title">좌석 선택 대기</h1>
          {message && <p className="card__msg">{message}</p>}
          {error && <p className="card__error">{error}</p>}

          <section className="queue">
            <div className="queue__circle">
              {position === 0 ? "입장" : position > 0 ? position : "—"}
            </div>
            <div className="queue__body">
              <div className="progress">
                <div className="progress__bar" style={{ width: `${progress}%` }} />
              </div>
              <div className="queue__meta">
                <span className="meta">
                  <span className="dot dot--live" /> 실시간 대기 중
                </span>
                {/* ⬇️ 실시간 반영되는 두 줄 */}
                <span className="meta">앞에 남은 인원: <b>{ahead}</b>명</span>
                <span className="meta">현재 대기 인원: <b>{waitingCount}</b>명</span>
              </div>
            </div>
          </section>

          <div className="hints">
            <span className="hint">새로고침하면 대기열이 밀리니 조심해주세요.</span>
            <span className="hint">대기열 - 매크로 인증 - 존 선택 - 좌석 선택 </span>
          </div>

          <div className="actions">
            <button className="btn btn--ghost" onClick={() => navigate("/home", { replace: true })}>
              다른 경기
            </button>
            <div className="grow" />
            <button className="btn btn--danger" onClick={cancelQueue}>대기 취소</button>
            <button className="btn btn--primary" disabled>
              대기 중…
            </button>
          </div>
        </main>

        <footer className="gate__foot">
          <span>안정적인 운영을 위해 순차 입장합니다.</span>
        </footer>
      </div>

      <div
        className={[
          "toast",
          toast.open ? "toast--show" : "toast--hide",
          `toast--${toast.type}`,
        ].join(" ")}
        role="status"
        aria-live="polite"
      >
        {toast.msg}
      </div>
    </div>
  );
}
