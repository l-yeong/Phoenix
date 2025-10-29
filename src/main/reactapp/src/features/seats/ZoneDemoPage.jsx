// src/pages/ZoneDemoPage.jsx
import React, { useEffect, useMemo, useState, useCallback, useRef } from "react";
import axios from "axios";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "../../styles/zone-seats.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://192.168.40.190:8080";
const api = axios.create({ baseURL: API, withCredentials: true });

const HOLD_TTL_SECONDS = 120;

async function leaveGateQuick({ gno, authHeaders = {} }) {
  const url = `${API}/gate/leave?gno=${encodeURIComponent(gno)}`;
  try {
    if (navigator.sendBeacon) {
      const blob = new Blob([String(gno)], { type: "application/json" });
      const ok = navigator.sendBeacon(url, blob);
      if (ok) return true;
    }
  } catch {}
  try {
    await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...authHeaders },
      credentials: "include",
      keepalive: true,
      body: JSON.stringify(gno),
    });
    return true;
  } catch { return false; }
}

function markKeepGateNext() { sessionStorage.setItem("gate_keep_next", "1"); }
function consumeKeepGateNext() {
  const keep = sessionStorage.getItem("gate_keep_next") === "1";
  if (keep) sessionStorage.removeItem("gate_keep_next");
  return keep;
}

export default function ZoneDemoPage() {
  const { zno } = useParams();
  const znoNum = Number(zno);
  const { state } = useLocation();
  const navigate = useNavigate();

  const gno = state?.gno ?? Number(sessionStorage.getItem("gate_gno") || 0);
  const zoneLabel = state?.zoneLabel ?? `ZNO ${zno}`;

  const authHeaders = useMemo(() => {
    const t = localStorage.getItem("jwt");
    return t ? { Authorization: `Bearer ${t}` } : {};
  }, []);

  const [gateTtl, setGateTtl] = useState(null);
  const leavingRef = useRef(false);

  useEffect(() => {
    if (!Number.isInteger(znoNum)) { alert("잘못된 존 URL입니다."); navigate("/gate", { replace: true }); return; }
    if (!Number.isInteger(Number(gno))) { navigate("/gate", { replace: true }); return; }
    sessionStorage.setItem("gate_gno", String(gno));

    let cancelled = false;
    let pollTimer = null;
    let tickTimer = null;

    const poll = async () => {
      if (cancelled) return;
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(gno)}`, { headers: { ...authHeaders } });
        const srvTtl = Number(data?.ttlSec ?? 0);
        setGateTtl(srvTtl);
        if (!data?.ready || srvTtl <= 0) {
          if (!leavingRef.current) {
            leavingRef.current = true;
            sessionStorage.removeItem("gate_gno");
            navigate(`/gate?expired=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
          }
        }
      } catch {}
      if (!cancelled) pollTimer = setTimeout(poll, 2000);
    };

    poll();
    tickTimer = setInterval(() => setGateTtl((v) => (v == null ? v : Math.max(0, v - 1))), 1000);

    const onUnload = () => { leaveGateQuick({ gno, authHeaders }); };
    window.addEventListener("beforeunload", onUnload);
    window.addEventListener("pagehide", onUnload);

    const onPop = () => { markKeepGateNext(); };
    window.addEventListener("popstate", onPop);

    return () => {
      cancelled = true;
      clearTimeout(pollTimer);
      clearInterval(tickTimer);
      window.removeEventListener("beforeunload", onUnload);
      window.removeEventListener("pagehide", onUnload);
      window.removeEventListener("popstate", onPop);

      const keep = consumeKeepGateNext();
      if (!keep && gno) leaveGateQuick({ gno, authHeaders });
    };
  }, [gno, znoNum, navigate, authHeaders]);

  const fmt = (s) => (s == null ? "--:--" : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`);

  const [seatsMeta, setSeatsMeta] = useState([]);
  const [metaErr, setMetaErr] = useState("");
  const loadSeatsMeta = useCallback(async () => {
    try {
      setMetaErr("");
      const { data } = await api.get(`/zone/${encodeURIComponent(znoNum)}/seats`, { headers: { ...authHeaders } });
      setSeatsMeta(Array.isArray(data?.seats) ? data.seats : []);
    } catch {
      setMetaErr("좌석 목록을 불러오지 못했습니다.");
      setSeatsMeta([]);
    }
  }, [znoNum, authHeaders]);
  useEffect(() => { loadSeatsMeta(); }, [loadSeatsMeta]);

  const [statusBySno, setStatusBySno] = useState({});
  const [remain, setRemain] = useState(null); // 남은 선택 가능 수(=확정+홀드 포함 4장 한도 기준)
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [onlyAvailable, setOnlyAvailable] = useState(false);

  const loadStatus = useCallback(async () => {
    if (seatsMeta.length === 0) return;
    setLoading(true); setErr("");
    try {
      const seatsPayload = seatsMeta.map((s) => ({ zno: znoNum, sno: s.sno }));
      const { data } = await api.post("/seat/status", { gno, seats: seatsPayload }, { headers: { "Content-Type": "application/json", ...authHeaders } });
      setStatusBySno(data?.statusBySno || {});
      if (typeof data?.remain === "number") setRemain(data.remain);
    } catch { setErr("좌석 상태를 불러오지 못했습니다."); }
    finally { setLoading(false); }
  }, [gno, znoNum, seatsMeta, authHeaders]);
  useEffect(() => { loadStatus(); }, [loadStatus]);

  // local TTLs
  const [myTtlBySno, setMyTtlBySno] = useState({});
  useEffect(() => {
    const t = setInterval(() => {
      setMyTtlBySno((prev) => {
        const next = {};
        for (const [k, v] of Object.entries(prev)) {
          const nv = Math.max(0, Number(v) - 1);
          if (nv > 0) next[k] = nv;
        }
        return next;
      });
    }, 1000);
    return () => clearInterval(t);
  }, []);

  const startLocalTtl = useCallback((sno, startedAtMs) => {
    const started = startedAtMs ?? Date.now();
    const elapsed = Math.floor((Date.now() - started) / 1000);
    const remain = Math.max(0, HOLD_TTL_SECONDS - elapsed);
    if (remain <= 0) return;
    setMyTtlBySno((prev) => ({ ...prev, [sno]: remain }));
    sessionStorage.setItem(`holdStartedAt:${gno}:${sno}`, String(started));
  }, [gno]);

  const clearLocalTtl = useCallback((sno) => {
    setMyTtlBySno((prev) => { const next = { ...prev }; delete next[sno]; return next; });
    sessionStorage.removeItem(`holdStartedAt:${gno}:${sno}`);
  }, [gno]);

  useEffect(() => {
    if (seatsMeta.length === 0) return;
    const now = Date.now();
    const baseline = Number(sessionStorage.getItem(`autoHoldBaselineAt:${gno}`) || 0);
    const defaultTtlSec = Number(sessionStorage.getItem(`autoHoldTtlSec:${gno}`) || HOLD_TTL_SECONDS);

    const restored = {};
    for (const s of seatsMeta) {
      const key = `holdStartedAt:${gno}:${s.sno}`;
      const started = Number(sessionStorage.getItem(key) || 0);
      const st = statusBySno[String(s.sno)];
      if (st === "HELD_BY_ME") {
        if (started > 0) {
          const elapsed = Math.floor((now - started) / 1000);
          const remain = Math.max(0, defaultTtlSec - elapsed);
          if (remain > 0) restored[s.sno] = remain; else sessionStorage.removeItem(key);
        } else if (baseline > 0) {
          const elapsed = Math.floor((now - baseline) / 1000);
          const remain = Math.max(0, defaultTtlSec - elapsed);
          if (remain > 0) {
            restored[s.sno] = remain;
            sessionStorage.setItem(key, String(now - (defaultTtlSec - remain) * 1000));
          }
        } else {
          sessionStorage.setItem(key, String(now));
          restored[s.sno] = defaultTtlSec;
        }
      }
    }
    if (Object.keys(restored).length) setMyTtlBySno((prev) => ({ ...prev, ...restored }));
  }, [gno, seatsMeta, statusBySno]);

  const gridSeats = useMemo(() => {
    const byName = [...seatsMeta].sort((a, b) => {
      const [ar, ac] = [a.seatName[0], Number(a.seatName.slice(1))];
      const [br, bc] = [b.seatName[0], Number(b.seatName.slice(1))];
      if (ar === br) return ac - bc;
      return ar.localeCompare(br);
    });
    return byName.map((s) => {
      const st = statusBySno[String(s.sno)] || "AVAILABLE";
      const ttl = st === "HELD_BY_ME" ? (myTtlBySno[String(s.sno)] ?? 0) : 0;
      return { ...s, st, ttl };
    });
  }, [seatsMeta, statusBySno, myTtlBySno, onlyAvailable]);

  // ★ Helper: fetch remaining selectable seats (optional UX)
  const fetchRemaining = useCallback(async () => {
    try {
      const { data } = await api.post("/seat/status", { gno, seats: [] }, { headers: { "Content-Type": "application/json", ...authHeaders } });
       return typeof data?.remain === "number" ? data.remain : null;
    } catch { return null; }
  }, [gno, authHeaders]);

  const toggleSeat = async ({ sno, st, seatName }) => {
    if (st === "SOLD" || st === "HELD" || st === "BLOCKED") {
      if (st === "BLOCKED") alert("시니어 전용석은 일반 예매에서 경기 2일 전부터 오픈됩니다.");
      return;
    }
    if (st === "HELD_BY_ME") {
      try {
        const { data } = await api.post("/seat/release", { gno, zno: znoNum, sno }, { headers: { "Content-Type": "application/json", ...authHeaders } });
        if (!data?.ok) return alert("해제 실패");
        clearLocalTtl(sno);
        await loadStatus();
      } catch { alert("네트워크 오류로 해제 실패"); }
      return;
    }
    try {
      const { data } = await api.post("/seat/select", { gno, zno: znoNum, sno }, { headers: { "Content-Type": "application/json", ...authHeaders } });
      if (!data?.ok) {
        // ★ updated mapping (no more -2)
        if (data?.code === -4) {
          const remain = await fetchRemaining();
          const extra = remain == null ? "" : `\n• 현재 추가 구매 가능: ${remain}장`;
          return alert(`한도 초과(최대 4장).\n확정 + 임시 보유 합이 4장을 넘을 수 없습니다.${extra}`);
        }
        const msg =
          data?.code === -1 ? "세션 없음" :
          data?.code === -3 ? "이미 홀드/매진" :
          data?.code === -5 ? "유효하지 않은 좌석" :
          data?.code === -6 ? "시니어 전용석은 일반 예매에서 경기 2일 전부터 오픈됩니다." :
          data?.msg || "선택 실패";
        return alert(msg);
      }
      startLocalTtl(sno, Date.now());
      await loadStatus();
    } catch { alert("네트워크 오류로 선택 실패"); }
  };

  const myHeldSnos = useMemo(() =>
    seatsMeta.map(s => s.sno).filter(sno => statusBySno[String(sno)] === "HELD_BY_ME"),
  [seatsMeta, statusBySno]);

  const confirmSeats = async () => {
    if (!myHeldSnos.length) { alert("임시 보유한 좌석이 없습니다."); return; }
    const ok = window.confirm(`${myHeldSnos.length}개 좌석을 결제(확정)하시겠습니까?`);
    if (!ok) return;
    try {
      const { data } = await api.post("/seat/confirm", { gno, snos: myHeldSnos }, { headers: { "Content-Type": "application/json", ...authHeaders } });
      if (data?.ok) {
        alert("결제(확정) 완료!");
        setMyTtlBySno({}); myHeldSnos.forEach((sno) => sessionStorage.removeItem(`holdStartedAt:${gno}:${sno}`));
        try {
          // ★ unify to query form
          await api.post(`/gate/leave?gno=${encodeURIComponent(gno)}`, null, { headers: { ...authHeaders } });
        } catch {}
        finally { sessionStorage.removeItem("gate_gno"); navigate("/home", { replace: true }); }
      }
      await loadStatus();
    } catch { alert("네트워크 오류로 결제에 실패했습니다."); }
  };

  const summary = useMemo(() => {
    let avail = 0, mine = 0, sold = 0, blocked = 0;
    for (const [, st] of Object.entries(statusBySno)) {
      if (st === "AVAILABLE") avail++;
      else if (st === "HELD_BY_ME") mine++;
      else if (st === "SOLD") sold++;
      else if (st === "BLOCKED") blocked++;
    }
    return { avail, mine, sold, blocked };
  }, [statusBySno]);

  return (
    <div className="zone-page">
      <div className="zone-header">
        <div className="zone-header__left">
          <h2 className="zone-title">좌석 선택 — {zoneLabel}</h2>
        </div>
        <div className="zone-header__right">
          <span className={`gate-ttl-badge ${gateTtl != null && gateTtl <= 30 ? "warn" : ""}`}>게이트 {fmt(gateTtl)}</span>
          <button onClick={loadStatus} className="btn btn--ghost" disabled={loading}>
            {loading ? "불러오는 중…" : "새로고침"}
          </button>
          <button
            onClick={() => {
              markKeepGateNext();
              navigate("/seats", { replace: true, state: { gno } });
            }}
            className="btn btn--ghost"
          >
            존 선택으로
          </button>
          <button onClick={confirmSeats} className="btn btn--primary" disabled={!myHeldSnos.length}>
            결제(확정) · {myHeldSnos.length}석
          </button>
        </div>
      </div>

      <div className="zone-toolbar">
        <Legend swatch="legend__swatch--green"   label="선택 가능 좌석" />
        <Legend swatch="legend__swatch--blue"    label="내 임시 좌석" />
        <Legend swatch="legend__swatch--hold"    label="임시 홀드 좌석" />
        <Legend swatch="legend__swatch--sold"    label="매진 좌석" />
        <Legend swatch="legend__swatch--blocked" label="시니어석(일반예매 D-2부터 오픈)" />
        <div className="zone-toolbar__spacer" />
        <div className="zone-filter">
          <label className="checkbox">
            <input type="checkbox" checked={onlyAvailable} onChange={(e) => setOnlyAvailable(e.target.checked)} />
            <span>선택 가능만</span>
          </label>
          <div className="zone-summary">
            남은 {summary.avail} · 내 임시 {summary.mine} · 매진 {summary.sold} · 잠금 {summary.blocked} <br/>
            {typeof remain === "number" && <> <strong>예매가능 좌석 수 : {remain}석</strong></>}
          </div>
        </div>
      </div>

      {(metaErr || err) && <div className="zone-error">{metaErr || err}</div>}

      {loading && (
        <div className="seat-grid">
          {Array.from({ length: 30 }).map((_, i) => (<div key={i} className="seat-skeleton" />))}
        </div>
      )}

      {!loading && (
        <div className="seat-grid">
          {gridSeats.map(({ sno, seatName, st, ttl }) => {
            const isGhost = onlyAvailable && st !== "AVAILABLE";
            const isDisabled =
              isGhost || st === "SOLD" || st === "HELD" || st === "BLOCKED";
            const titleText = isGhost
              ? ""
              : st === "BLOCKED"
              ? `${seatName} · 시니어 전용석 (일반 예매 D-2부터)`
              : `${seatName} · ${st}`;

            return (
              <button
                key={sno}
                onClick={() => { if (!isGhost) toggleSeat({ sno, st, seatName }); }}
                disabled={isDisabled}
                title={titleText}
                aria-hidden={isGhost || undefined}
                tabIndex={isGhost ? -1 : 0}
                className={`seat-chip seat-chip--${st.toLowerCase()} ${isGhost ? "seat-chip--ghost" : ""}`}
              >
                <span className="seat-label">{seatName}</span>
                {!isGhost && st === "HELD_BY_ME" && (ttl ?? 0) > 0 && (
                  <span className="ttl-badge ttl-badge--mine">{ttl}s</span>
                )}
              </button>
            );
          })}
        </div>
      )}

      <div className="zone-footer-note">
        • 임시 보유는 2분 후 자동 해제됩니다. (표시 시간은 클라이언트 기준, 새로고침 시 서버와 동기화) <br />
        • 새로고침/뒤로가기/창닫기 시 Gate로 복귀하여 다시 대기열에 등록됩니다.
      </div>
    </div>
  );
}

function Legend({ swatch, label }) {
  return (
    <div className="legend">
      <span className={`legend__swatch ${swatch}`} />
      <span className="legend__label">{label}</span>
    </div>
  );
}
