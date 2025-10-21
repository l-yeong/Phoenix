import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import "../../styles/gate.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function GatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ✅ mno / gno는 모두 "정수"
  const [mno, setMno] = useState(Number(sessionStorage.getItem("gate_mno")) || 20001);
  const [gno, setGno] = useState(Number(sessionStorage.getItem("gate_gno")) || 143);


  const [queued, setQueued] = useState(sessionStorage.getItem("gate_queued") === "1");
  const [waitingCount, setWaitingCount] = useState(0);
  const [myPosition, setMyPosition] = useState(null);
  const [msg, setMsg] = useState("");
  const [loading, setLoading] = useState(false);
  const [ready, setReady] = useState(false);

  // 세션스토리지 동기화
  useEffect(() => sessionStorage.setItem("gate_mno", String(mno)), [mno]);
  useEffect(() => sessionStorage.setItem("gate_gno", String(gno)), [gno]);

  // 다른 페이지에서 넘어온 state 적용
  useEffect(() => {
    if (location.state?.mno) setMno(Number(location.state.mno));
    if (location.state?.gno) setGno(Number(location.state.gno));
  }, [location.state]);

  // state로 받은 값이 있고 아직 큐 미등록이면 자동 등록
  useEffect(() => {
    if (location.state?.mno && location.state?.gno && !queued) {
      enqueue();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state, mno, gno]);

  // ✅ 대기열 등록
  const enqueue = async () => {
    setLoading(true);
    setMsg("");
    try {
      const payload = { mno: Number(mno), gno: Number(gno) }; // ← 백엔드 DTO와 동일 키/타입
      const { data } = await api.post("/gate/enqueue", payload);
      const q = Boolean(data?.queued);
      setQueued(q);
      sessionStorage.setItem("gate_queued", q ? "1" : "0");
      setWaitingCount(Number(data?.waiting) || 0);
      setMsg(q ? `대기열 등록 완료! (현재 ${data.waiting}명 대기 중)` : "이미 예매했거나 등록 불가합니다.");
    } catch (e) {
      console.error(e);
      setMsg("대기열 등록 중 오류가 발생했습니다.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ 1초마다 상태 폴링 (세션 alive + 내 순번 + 대기열 길이)
  useEffect(() => {
    if (!queued) return;
    const timer = setInterval(async () => {
      try {
        const { data: check } = await api.get(`/gate/check/${encodeURIComponent(mno)}`);
        const isReady = Boolean(check?.ready);
        setReady(isReady);
        // console.log("[GATE] ready=", isReady);

        const { data: pos } = await api.get(`/gate/position/${encodeURIComponent(mno)}`);
        if (typeof pos?.position === "number") setMyPosition(pos.position);

        const { data: status } = await api.get("/gate/status");
        setWaitingCount(Number(status?.waiting) || 0);
      } catch (e) {
        // 네트워크 오류는 일시 무시
      }
    }, 1000); // ← 1s 폴링
    return () => clearInterval(timer);
  }, [api, queued, mno]);

  // ✅ 보강: ready가 true로 변하는 "순간" 강제 리디렉트
  useEffect(() => {
    if (ready) {
      // replace: 뒤로가기 시 대기실로 못 돌아오게 하려면 true 유지
      navigate("/macro", { replace: true, state: { mno, gno } });
    }
  }, [ready, navigate, mno, gno]);

  return (
    <div className="gate-wrap">
      <div className="gate-title">
        <span className="emoji">🎟️</span> 예매 대기실
      </div>

      {/* 입장 전 화면 */}
      {!queued && (
        <>
          <div className="gate-field">
            <label className="gate-label">사용자 번호 (mno)</label>
            <input
              className="gate-input"
              value={mno}
              onChange={(e) => setMno(Number(e.target.value) || 0)}
              type="number"
              min="1"
            />
          </div>
          <div className="gate-field">
            <label className="gate-label">경기 번호 (gno)</label>
            <input
              className="gate-input"
              value={gno}
              onChange={(e) => setGno(Number(e.target.value) || 0)}
              type="number"
              min="1"
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
                <span className="gate-ready">입장 준비 완료! 곧 이동합니다…</span>
              ) : (
                <span className="gate-wait">대기 중...</span>
              )}
            </span>

            {typeof myPosition === "number" && myPosition >= 0 ? (
              <span>
                내 순번: <b>{myPosition}</b>
              </span>
            ) : (
              <span>
                대기 인원: <b>{waitingCount}</b>
              </span>
            )}
          </div>

          <div className="gate-msg">
            순서가 되면 자동으로 <b>/macro</b> 페이지로 이동합니다.
          </div>
        </div>
      )}

      <div className="gate-foot">
        • “대기열 입장” 후 1초마다 내 순번이 자동 갱신됩니다. <br />
        • 입장 준비 완료 시 자동으로 <b>/macro</b> 페이지로 이동합니다.
      </div>
    </div>
  );
}
