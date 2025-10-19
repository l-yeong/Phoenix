import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../styles/gate.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function GatePage() {
  const navigate = useNavigate();
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  const [userId, setUserId] = useState(sessionStorage.getItem("gate_userId") || "u123");
  const [showId, setShowId] = useState(sessionStorage.getItem("gate_showId") || "SHOW-2025-10-16-19:00");
  const [queued, setQueued] = useState(sessionStorage.getItem("gate_queued") === "1");
  const [waitingCount, setWaitingCount] = useState(0);
  const [availablePermits, setAvailablePermits] = useState(0);
  const [myPosition, setMyPosition] = useState(null);
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [ready, setReady] = useState(false);

  useEffect(() => sessionStorage.setItem("gate_userId", userId), [userId]);
  useEffect(() => sessionStorage.setItem("gate_showId", showId), [showId]);

  const enqueue = async () => {
    setLoading(true); setMsg("");
    try {
      const { data } = await api.post("/gate/enqueue", { userId, showId });
      setQueued(Boolean(data.queued));
      sessionStorage.setItem("gate_queued", data.queued ? "1" : "0");
      setWaitingCount(Number(data.waiting) || 0);
      setMsg(data.queued ? `대기열 등록 완료 (현재 대기 ${data.waiting}명)` : "이미 예매했거나 등록 불가");
    } catch {
      setMsg("대기열 등록 실패");
    } finally { setLoading(false); }
  };

  useEffect(() => {
    if (!queued) return;
    const t = setInterval(async () => {
      try {
        const { data: check } = await api.get(`/gate/check/${encodeURIComponent(userId)}`);
        setReady(Boolean(check?.ready));
        if (check?.ready) {
          navigate("/macro", { state: { userId, showId } });
          return;
        }
        try {
          const { data: pos } = await api.get(`/gate/position/${encodeURIComponent(userId)}`);
          if (typeof pos?.position === "number") setMyPosition(pos.position);
        } catch {}
        const { data: status } = await api.get("/gate/status");
        setWaitingCount(Number(status?.waiting) || 0);
        setAvailablePermits(Number(status?.availablePermits) || 0);
      } catch {}
    }, 2000);
    return () => clearInterval(t);
  }, [api, queued, userId, showId, navigate]);

  return (
    <div className="gate-wrap">
      <div className="gate-title"><span className="emoji">🎟️</span> 대기실</div>

      {!queued && (
        <>
          <div className="gate-field">
            <label className="gate-label">User ID</label>
            <input className="gate-input" value={userId} onChange={(e)=>setUserId(e.target.value)} />
          </div>
          <div className="gate-field">
            <label className="gate-label">Show ID</label>
            <input className="gate-input" value={showId} onChange={(e)=>setShowId(e.target.value)} />
          </div>
          <div className="gate-actions">
            <button className="gate-btn" disabled={loading} onClick={enqueue}>
              {loading ? "등록 중..." : "대기열 입장"}
            </button>
          </div>
          {msg && <div className="gate-msg">{msg}</div>}
        </>
      )}

      {queued && (
        <div className="gate-card">
          <div className="gate-meta">
            <span>내 상태: {ready ? <span className="gate-ready">입장 준비됨</span> : <span className="gate-wait">대기 중</span>}</span>
            {typeof myPosition === "number" && myPosition >= 0 ? (
              <span>내 순번: <b>{myPosition}</b></span>
            ) : (
              <span>대기열 길이: <b>{waitingCount}</b></span>
            )}
            <span>남은 슬롯: <b>{availablePermits}</b></span>
          </div>
          <div className="gate-msg">내 차례가 되면 자동으로 보안 검증 페이지로 이동합니다.</div>
        </div>
      )}

      {msg && queued && <div className="gate-msg">{msg}</div>}

      <div className="gate-foot">
        • “대기열 입장” 후 2초마다 내 순번/현황을 갱신합니다. <br/>
        • ready가 되면 자동으로 <b>/macro</b>로 이동합니다.
      </div>
    </div>
  );
}
