import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import "../../styles/gate.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function GatePage() {
  const navigate = useNavigate();
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ✅ uno / gno (사용자 ID, 공연 ID)
  const [uno, setUno] = useState(sessionStorage.getItem("gate_uno") || "1");
  const [gno, setGno] = useState(sessionStorage.getItem("gate_gno") || "SHOW-2025-10-16-19:00");
  const [queued, setQueued] = useState(sessionStorage.getItem("gate_queued") === "1");
  const [waitingCount, setWaitingCount] = useState(0);
  const [myPosition, setMyPosition] = useState(null);
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [ready, setReady] = useState(false);


  // 예매페이지 들어가기전 uno gno 받아야와야함 

  // 세션스토리지에 ID 저장
  useEffect(() => sessionStorage.setItem("gate_uno", uno), [uno]);
  useEffect(() => sessionStorage.setItem("gate_gno", gno), [gno]);

  // ✅ 대기열 등록
  const enqueue = async () => {
    setLoading(true);
    setMsg("");
    try {
      const { data } = await api.post("/gate/enqueue", { uno, gno });
      setQueued(Boolean(data.queued));
      sessionStorage.setItem("gate_queued", data.queued ? "1" : "0");
      setWaitingCount(Number(data.waiting) || 0);
      setMsg(
        data.queued
          ? `대기열 등록 완료! (현재 ${data.waiting}명 대기 중)`
          : "이미 예매했거나 등록할 수 없습니다."
      );
    } catch {
      setMsg("대기열 등록 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 2초마다 상태 확인 (내 순번 + 입장 여부)
  useEffect(() => {
    if (!queued) return;
    const timer = setInterval(async () => {
      try {
        // 1️⃣ 게이트 세션 확인
        const { data: check } = await api.get(`/gate/check/${encodeURIComponent(uno)}`);
        setReady(Boolean(check?.ready));
        if (check?.ready) {
          navigate("/macro", { state: { uno, gno } });
          return;
        }

        // 2️⃣ 내 순번 조회
        const { data: pos } = await api.get(`/gate/position/${encodeURIComponent(uno)}`);
        if (typeof pos?.position === "number") setMyPosition(pos.position);

        // 3️⃣ 대기열 길이 (백엔드에서 관리)
        const { data: status } = await api.get("/gate/status");
        setWaitingCount(Number(status?.waiting) || 0);
      } catch {
        // 네트워크 오류 시 무시
      }
    }, 2000);
    return () => clearInterval(timer);
  }, [api, queued, uno, gno, navigate]);

  return (
    <div className="gate-wrap">
      <div className="gate-title">
        <span className="emoji">🎟️</span> 예매 대기실
      </div>

      {/* 입장 전 화면 */}
      {!queued && (
        <>
          <div className="gate-field">
            <label className="gate-label">사용자 번호 (uno)</label>
            <input
              className="gate-input"
              value={uno}
              onChange={(e) => setUno(e.target.value)}
            />
          </div>
          <div className="gate-field">
            <label className="gate-label">공연 번호 (gno)</label>
            <input
              className="gate-input"
              value={gno}
              onChange={(e) => setGno(e.target.value)}
            />
          </div>
          <div className="gate-actions">
            <button className="gate-btn" disabled={loading} onClick={enqueue}>
              {loading ? "등록 중..." : "대기열 입장"}
            </button>
          </div>
          {msg && <div className="gate-msg">{msg}</div>}
        </>
      )}

      {/* 대기 중 화면 */}
      {queued && (
        <div className="gate-card">
          <div className="gate-meta">
            <span>
              내 상태:{" "}
              {ready ? (
                <span className="gate-ready">입장 준비 완료!</span>
              ) : (
                <span className="gate-wait">대기 중...</span>
              )}
            </span>

            {typeof myPosition === "number" && myPosition >= 0 ? (
              <span>
                내 순번: <b>{myPosition}</b>
              </span>
            ) : (
              <span>대기 중 인원: <b>{waitingCount}</b></span>
            )}
          </div>

          <div className="gate-msg">
            순서가 되면 자동으로 <b>보안 검증 페이지</b>로 이동합니다.
          </div>
        </div>
      )}

      <div className="gate-foot">
        • “대기열 입장” 후 2초마다 내 순번이 자동 갱신됩니다. <br />
        • 입장 준비 완료 시 자동으로 <b>/macro</b> 페이지로 이동합니다.
      </div>
    </div>
  );
}
