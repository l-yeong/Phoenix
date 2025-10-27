// src/pages/SeatsPolygonPage.jsx
import React, { useEffect, useMemo, useState, useCallback, useRef } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import ZoomableSvgOverlay from "../../components/ZoomableSvgOverlay";
import zonesData from "../../data/zones.json";
import "../../styles/seats-polygon.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const api = axios.create({ baseURL: API, withCredentials: true });

const HOLD_TTL_SECONDS = 120;

/** 공통: 게이트 leave (컨트롤러에 맞춰 ?gno= 로 통일) */
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

export default function SeatsPolygonPage() {
  const navigate = useNavigate();
  const { state } = useLocation(); // { gno } 기대
  const gno = state?.gno ?? Number(sessionStorage.getItem("gate_gno") || 0);

  // ───────── 가드(추가분) ─────────
  const authHeaders = useMemo(() => {
    const t = localStorage.getItem("jwt");
    return t ? { Authorization: `Bearer ${t}` } : {};
  }, []);
  const [gateTtl, setGateTtl] = useState(null);
  const leavingRef = useRef(false);

  useEffect(() => {
    if (!Number.isInteger(Number(gno))) {
      navigate("/gate", { replace: true });
      return;
    }
    sessionStorage.setItem("gate_gno", String(gno));

    let cancelled = false;
    let pollTimer = null;
    let tickTimer = null;

    // ▶ 초기 1회 체크: 새로고침 직후 등 세션 없으면 곧장 게이트로 (requeue)
    (async () => {
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(gno)}`, { headers: { ...authHeaders } });
        const ok = !!data?.ready;
        const srvTtl = Number(data?.ttlSec ?? 0);
        setGateTtl(srvTtl);
        if (!ok || srvTtl <= 0) {
          if (!leavingRef.current) {
            leavingRef.current = true;
            sessionStorage.removeItem("gate_gno");
            navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
          }
          return; // 세션 없으면 폴링 시작하지 않음
        }
      } catch {
        // 네트워크 순간 오류는 폴링에서 다시 시도
      }
    })();

    // ▶ 폴링: 살아있는 동안 TTL 갱신, 만료되면 expired로
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

    // ▶ TTL 표시용 로컬 tick
    tickTimer = setInterval(() => setGateTtl(v => (v == null ? v : Math.max(0, v - 1))), 1000);

    // ▶ 새로고침/창닫기 → leave (다음 로드에서 requeue 가드가 동작)
    const onUnload = () => { leaveGateQuick({ gno, authHeaders }); };
    window.addEventListener("beforeunload", onUnload);
    window.addEventListener("pagehide", onUnload);

    // ▶ 뒤로가기 → leave + 즉시 게이트 requeue로
    const onPop = () => {
      leaveGateQuick({ gno, authHeaders });
      if (!leavingRef.current) {
        leavingRef.current = true;
        sessionStorage.removeItem("gate_gno");
        navigate(`/gate?requeue=1&gno=${encodeURIComponent(gno)}`, { replace: true, state: { gno } });
      }
    };
    window.addEventListener("popstate", onPop);

    return () => {
      cancelled = true;
      clearTimeout(pollTimer);
      clearInterval(tickTimer);
      window.removeEventListener("beforeunload", onUnload);
      window.removeEventListener("pagehide", onUnload);
      window.removeEventListener("popstate", onPop);

      // 내부 이동(→ /zone/:zno 등)만 유지, 그 외에는 leave
      const keep = consumeKeepGateNext();
      if (!keep && gno) leaveGateQuick({ gno, authHeaders });
    };
  }, [gno, navigate, authHeaders]);
  // ───────── 가드(추가분) 끝 ─────────

  const fmt = (s) => (s == null ? "--:--" : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`);

  const zones = useMemo(() => zonesData, []);

  // ─────────────────────────────────────────────
  // 존별 남은 좌석(AVAILABLE) & 시니어석(BLOCKED) 집계
  // ─────────────────────────────────────────────
  const [remainByZone, setRemainByZone] = useState({});
  const [loadingMap, setLoadingMap] = useState(false);
  const [mapErr, setMapErr] = useState("");

  const loadRemainForZone = useCallback(async (zno) => {
    const { data: meta } = await api.get(`/zone/${encodeURIComponent(zno)}/seats`);
    const seats = Array.isArray(meta?.seats) ? meta.seats : [];
    if (seats.length === 0) return { avail: 0, blocked: 0 };

    const seatsPayload = seats.map((s) => ({ zno: Number(zno), sno: s.sno }));
    const { data: stat } = await api.post("/seat/status", { gno, seats: seatsPayload });
    const statusBySno = stat?.statusBySno || {};

    let avail = 0, blocked = 0;
    for (const st of Object.values(statusBySno)) {
      if (st === "AVAILABLE") avail++;
      else if (st === "BLOCKED") blocked++;
    }
    return { avail, blocked };
  }, [gno]);

  const loadAllZonesRemain = useCallback(async () => {
    if (!Number.isInteger(Number(gno))) return;
    setLoadingMap(true);
    setMapErr("");
    try {
      const results = await Promise.allSettled(
        zones.map(async (z) => [z.id, await loadRemainForZone(z.zno)])
      );
      const acc = {};
      for (const r of results) if (r.status === "fulfilled") {
        const [id, obj] = r.value;
        acc[id] = obj ?? { avail: 0, blocked: 0 };
      }
      setRemainByZone(acc);
    } catch (e) {
      console.error(e);
      setMapErr("존별 남은 좌석을 불러오지 못했습니다.");
      setRemainByZone({});
    } finally {
      setLoadingMap(false);
    }
  }, [gno, zones, loadRemainForZone]);

  useEffect(() => { loadAllZonesRemain(); }, [loadAllZonesRemain]);

  // ─────────────────────────────────────────────
  // 자동예매 (멀티-존 결과 지원)
  // ─────────────────────────────────────────────
  const [autoRes, setAutoRes] = useState(null);
  const [ttlLeft, setTtlLeft] = useState(0);

  const pendingZoneMap = useMemo(() => {
    if (!autoRes?.bundles) return {};
    const acc = {};
    for (const b of autoRes.bundles) acc[b.zno] = (acc[b.zno] || 0) + (b.snos?.length || 0);
    return acc;
  }, [autoRes]);

  useEffect(() => {
    if (!autoRes?.ttlSec) return;
    setTtlLeft(autoRes.ttlSec);
    const t = setInterval(() => setTtlLeft((v) => (v > 0 ? v - 1 : 0)), 1000);
    return () => clearInterval(t);
  }, [autoRes?.ttlSec]);
  useEffect(() => { if (ttlLeft === 0 && autoRes) setAutoRes(null); }, [ttlLeft, autoRes]);

  const [qty, setQty] = useState(2);
  const [preferContiguous, setPreferContiguous] = useState(true);
  const [fanSide, setFanSide] = useState("HOME"); // HOME | AWAY | ANY

  const mapAutoReasonToKo = (reason) => {
    if (!reason) return "좌석을 찾지 못했습니다.";
    if (reason.startsWith("QTY_OUT_OF_RANGE")) return "매수는 1~4장만 선택할 수 있어요.";
    switch (reason) {
      case "GAME_NOT_FOUND":       return "경기 정보를 찾지 못했어요.";
      case "NO_SEATS_MATCH_RULES": return "조건에 맞는 좌석을 찾지 못했어요.";
      case "PARTIAL":              return "요청 수량만큼은 못 찾았어요.";
      default:                     return "좌석을 찾지 못했어요.";
    }
  };
  const pickHintFromStrategy = (strategy, { preferContiguous }) => {
    if (!strategy) return null;
    if (/lock-fail:-6/.test(strategy)) return "시니어 전용석은 일반예매에서 경기 2일 전부터 오픈됩니다.";
    if (/lock-fail:-4/.test(strategy)) return "이미 보유 좌석이 있거나 한도(4장)를 초과했을 수 있어요.";
    if (/lock-fail:-3|singles-failed@|contiguous-failed@/.test(strategy)) return "다른 사용자가 선점 중일 수 있어요.";
    if (preferContiguous && /no-run@/.test(strategy)) return "연석이 부족해요. ‘연석 우선’을 끄거나 매수를 줄이면 성공 확률↑";
    return null;
  };
  const humanizeAutoFail = (data, opts) => {
    const base = mapAutoReasonToKo(data?.reason);
    const hint = pickHintFromStrategy(data?.strategy || "", opts);
    return hint ? `${base}\n• ${hint}` : base;
  };

  const stampAutoHoldTimestamps = useCallback((bundles, ttlSec) => {
    sessionStorage.setItem(`autoHoldBaselineAt:${gno}`, String(Date.now()));
    for (const b of bundles) for (const sno of (b.snos || []))
      sessionStorage.setItem(`holdStartedAt:${gno}:${sno}`, String(Date.now()));
    sessionStorage.setItem(`autoHoldTtlSec:${gno}`, String(ttlSec || HOLD_TTL_SECONDS));
  }, [gno]);

  const runAutoSelect = useCallback(async () => {
    try {
      const payload = { gno, qty, preferContiguous, fanSide };
      const { data } = await api.post("/seat/auto", payload);
      if (!data?.ok) { alert(humanizeAutoFail(data, { preferContiguous })); return; }

      let bundles = Array.isArray(data?.bundles) ? data.bundles : [];
      if ((!bundles || bundles.length === 0) && data?.zno && Array.isArray(data?.heldSnos)) {
        bundles = [{ zno: data.zno, zoneLabel: data.zoneLabel || `ZNO ${data.zno}`, contiguous: !!data.contiguous, snos: data.heldSnos, seatNames: [] }];
      }
      const ttlSec = data?.ttlSec ?? HOLD_TTL_SECONDS;
      stampAutoHoldTimestamps(bundles, ttlSec);

      if ((bundles?.length || 0) === 1) {
        const b = bundles[0];
        markKeepGateNext(); // 내부 이동 → leave 스킵
        sessionStorage.setItem("gate_gno", String(gno));
        navigate(`/zone/${b.zno}`, { state: { gno, zno: b.zno, zoneLabel: b.zoneLabel || `ZNO ${b.zno}` }, replace: false });
        return;
      }
      setAutoRes({ qty: data.qty ?? qty, ttlSec, bundles: bundles || [] });
    } catch (e) {
      console.error(e);
      alert("자동예매 실패: 네트워크 오류");
    }
  }, [gno, qty, preferContiguous, fanSide, navigate, stampAutoHoldTimestamps]);

  const chooseZone = useCallback((bundle) => {
    setAutoRes(null);
    markKeepGateNext(); // 내부 이동
    sessionStorage.setItem("gate_gno", String(gno));
    navigate(`/zone/${bundle.zno}`, { state: { gno, zno: bundle.zno, zoneLabel: bundle.zoneLabel || `ZNO ${bundle.zno}` }, replace: false });
  }, [gno, navigate]);

  const releaseAllAuto = useCallback(async () => {
    try {
      if (!autoRes?.bundles?.length) return;
      const tasks = [];
      for (const b of autoRes.bundles) for (const sno of (b.snos || []))
        tasks.push(api.post("/seat/release", { gno, zno: b.zno, sno }));
      await Promise.allSettled(tasks);
    } catch {} finally { setAutoRes(null); }
  }, [autoRes, gno]);

  // 툴팁
  const [tooltip, setTooltip] = useState(null);
  const handleZoneHover = (zone, e) => {
    const info = remainByZone[zone.id] || { avail: 0, blocked: 0 };
    setTooltip({ x: e.clientX, y: e.clientY, label: zone.label, remainAvail: info.avail, remainBlocked: info.blocked });
  };
  const handleZoneLeave = () => setTooltip(null);

  // 존 클릭 → 상세 (내부 이동 플래그)
  const goZoneDetail = (zone) => {
    if (!zone || !Number.isInteger(Number(zone.zno))) return alert("존 정보가 올바르지 않습니다.");
    markKeepGateNext();
    sessionStorage.setItem("gate_gno", String(gno));
    navigate(`/zone/${Number(zone.zno)}`, { state: { gno: Number(gno), zoneId: zone.id, zno: Number(zone.zno), zoneLabel: zone.label }, replace: false });
  };

  return (
    <div className="seats-layout">
      <div className="seats-canvas">
        <div className="seats-head">
          <h2>🎟️ 좌석 현황</h2>
          <span className="meta">경기번호: {gno}</span>
          <span className={`gate-ttl-badge ${gateTtl != null && gateTtl <= 30 ? "warn" : ""}`}>게이트 {fmt(gateTtl)}</span>
          <span className="spacer" />
          <button className="ghost-btn" onClick={loadAllZonesRemain} disabled={loadingMap}>
            {loadingMap ? "갱신 중…" : "새로고침"}
          </button>
        </div>

        <div className="canvas-wrap canvas-wrap--contain">
          <ZoomableSvgOverlay
            backgroundUrl="/stadium.png"
            zones={zones}
            onZoneClick={goZoneDetail}
            onZoneHover={handleZoneHover}
            onZoneLeave={handleZoneLeave}
            fit="contain"
            preserveAspectRatio="xMidYMid meet"
          />
        </div>

        {tooltip && (
          <div className="zone-tooltip" style={{ top: tooltip.y + 12, left: tooltip.x + 12 }}>
            <div className="tt-title">{tooltip.label}</div>
            <div className="tt-sub">선택 가능 {tooltip.remainAvail}석</div>
            {tooltip.remainBlocked > 0 && <div className="tt-sub muted">시니어석 {tooltip.remainBlocked}석 (일반 D-2부터)</div>}
          </div>
        )}
      </div>

      <aside className="seats-side">
        <div className="side-card">
          <div className="side-title">존별 남은 좌석</div>
          {mapErr && <div className="side-error">{mapErr}</div>}
          <ul className="zone-list">
            {zones.map((z) => {
              const info = remainByZone[z.id] || { avail: 0, blocked: 0 };
              const isSeniorOnly = info.avail === 0 && info.blocked > 0;
              const secured = (autoRes?.bundles || []).reduce((n, b) => n + (b.zno === z.zno ? (b.snos?.length || 0) : 0), 0);
              return (
                <li
                  key={z.id}
                  className={`zone-item ${isSeniorOnly ? "zone-item--locked" : ""}`}
                  onClick={() => goZoneDetail(z)}
                  role="button"
                  title={isSeniorOnly ? `${z.label} — 시니어석만 남음 (일반예매 D-2부터)` : `${z.label} 남은 좌석 ${info.avail}석`}
                >
                  <span className="zone-label-wrap">
                    <span className="zone-label">{z.label}</span>
                    {info.blocked > 0 && (
                      <span className="badge-row">
                        <span className="zone-badge zone-badge--blocked">
                          <span className="zone-badge__dot" aria-hidden="true" />
                          시니어 전용 {info.blocked}석
                          <span className="tooltip">일반예매는 경기 D-2 오픈</span>
                        </span>
                      </span>
                    )}
                  </span>

                  <span className="zone-right">
                    {!!secured && <span className="zone-badge zone-badge--secured" title="자동예매로 확보된 좌석">확보 {secured}</span>}
                    <span className="zone-count">{loadingMap ? "…" : info.avail}<span className="unit">석</span></span>
                  </span>
                </li>
              );
            })}
          </ul>
        </div>

        <div className="side-card auto-card sticky-bottom">
          <div className="side-title">
            자동예매
            <span className={`gate-ttl-mini ${gateTtl != null && gateTtl <= 30 ? "warn" : ""}`}>{fmt(gateTtl)}</span>
          </div>
          <div className="auto-opts">
            <label className="auto-field">
              <span>매수</span>
              <select value={qty} onChange={(e) => setQty(Number(e.target.value))}>
                <option value={1}>1</option><option value={2}>2</option><option value={3}>3</option><option value={4}>4</option>
              </select>
            </label>
            <label className="checkbox auto-check">
              <input type="checkbox" checked={preferContiguous} onChange={(e) => setPreferContiguous(e.target.checked)} />
              <span>연석 우선</span>
            </label>
            <label className="auto-field">
              <span>팬사이드</span>
              <select value={fanSide} onChange={(e) => setFanSide(e.target.value)}>
                <option value="HOME">홈</option><option value="AWAY">어웨이</option><option value="ANY">상관없음</option>
              </select>
            </label>
          </div>
          <div className="auto-actions">
            {autoRes?.bundles?.length > 1 && <button className="ghost-btn" onClick={releaseAllAuto}>모두 해제</button>}
            <button className="primary-btn" onClick={runAutoSelect}>자동예매</button>
          </div>
        </div>
      </aside>

      {autoRes?.bundles?.length > 1 && (
        <div className="auto-result-bar">
          <div className="auto-result-left">
            <div className="auto-result-title">자동예매 결과</div>
            <div className="auto-result-meta">총 {autoRes.qty}석 확보 · <strong>{ttlLeft}s</strong> 내 결정 필요</div>
          </div>
          <div className="auto-result-bundles">
            {autoRes.bundles.map((b) => (
              <button key={b.zno} className="bundle-chip" onClick={() => chooseZone(b)}
                title={`${b.zoneLabel || `ZNO ${b.zno}`} / ${b.contiguous ? "연석" : "비연석"} / ${b.snos?.length || 0}석`}>
                <span className="bundle-chip__label">{b.zoneLabel || `ZNO ${b.zno}`}</span>
                <span className="bundle-chip__count">{b.snos?.length || 0}석</span>
                {b.contiguous && <span className="bundle-chip__tag">연석</span>}
              </button>
            ))}
          </div>
          <div className="auto-result-actions">
            <button className="ghost-btn" onClick={releaseAllAuto}>모두 해제</button>
          </div>
        </div>
      )}
    </div>
  );
}
