import React, { useEffect, useMemo, useState, useCallback } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import ZoomableSvgOverlay from "../../components/ZoomableSvgOverlay";
import zonesData from "../../data/zones.json";
import "../../styles/seats-polygon.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const api = axios.create({ baseURL: API, withCredentials: true });

export default function SeatsPolygonPage() {
  const navigate = useNavigate();
  const { state } = useLocation(); // { gno } 기대
  const gno = state?.gno ?? Number(sessionStorage.getItem("gate_gno") || 0);

  // ✅ Gate 세션 가드
  useEffect(() => {
    if (!Number.isInteger(Number(gno))) {
      navigate("/gate", { replace: true });
      return;
    }
    sessionStorage.setItem("gate_gno", String(gno));
    let cancelled = false;
    (async () => {
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(gno)}`);
        if (!cancelled && (!data || data.ready === false)) navigate("/gate", { replace: true });
      } catch {
        if (!cancelled) navigate("/gate", { replace: true });
      }
    })();
    return () => { cancelled = true; };
  }, [gno, navigate]);

  const zones = useMemo(() => zonesData, []);

  // ✅ 존별 남은 좌석
  const [remainByZone, setRemainByZone] = useState({});
  const [loadingMap, setLoadingMap] = useState(false);
  const [mapErr, setMapErr] = useState("");

  const loadRemainForZone = useCallback(async (zno) => {
    // 1) 존 좌석 리스트
    const { data: meta } = await api.get(`/zone/${encodeURIComponent(zno)}/seats`);
    const seats = Array.isArray(meta?.seats) ? meta.seats : [];
    if (seats.length === 0) return 0;

    // 2) 해당 sno만 상태 요청
    const seatsPayload = seats.map((s) => ({ zno: Number(zno), sno: s.sno }));
    const { data: stat } = await api.post("/seat/status", { gno, seats: seatsPayload });
    const statusBySno = stat?.statusBySno || {};
    return Object.values(statusBySno).filter((st) => st === "AVAILABLE").length;
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
      for (const r of results) {
        if (r.status === "fulfilled") {
          const [id, cnt] = r.value;
          acc[id] = cnt ?? 0;
        }
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

  // ✅ 툴팁
  const [tooltip, setTooltip] = useState(null);
  const handleZoneHover = (zone, e) => {
    setTooltip({
      x: e.clientX,
      y: e.clientY,
      label: zone.label,
      remain: remainByZone[zone.id] ?? 0,
    });
  };
  const handleZoneLeave = () => setTooltip(null);

  // ✅ 클릭 → 존 상세로 이동
  const goZoneDetail = (zone) => {
    if (!zone || !Number.isInteger(Number(zone.zno))) {
      alert("존 정보가 올바르지 않습니다.");
      return;
    }
    sessionStorage.setItem("gate_gno", String(gno));
    navigate(`/zone/${Number(zone.zno)}`, {
      state: { gno: Number(gno), zoneId: zone.id, zno: Number(zone.zno), zoneLabel: zone.label },
    });
  };

  return (
    <div className="seats-layout">
      <div className="seats-canvas">
        <div className="seats-head">
          <h2>🎟️ 좌석 현황</h2>
          <span className="meta">경기번호: {gno}</span>
          <span className="spacer" />
          <button className="ghost-btn" onClick={loadAllZonesRemain} disabled={loadingMap}>
            {loadingMap ? "갱신 중…" : "새로고침"}
          </button>
        </div>

        {/* ✅ 배경 / 폴리곤 레이어 분리 (잘림/어긋남 방지) */}
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
            <div className="tt-sub">남은 좌석 {tooltip.remain}석</div>
          </div>
        )}
      </div>

      <aside className="seats-side">
        <div className="side-card">
          <div className="side-title">존별 남은 좌석</div>
          {mapErr && <div className="side-error">{mapErr}</div>}
          <ul className="zone-list">
            {zones.map((z) => {
              const remain = remainByZone[z.id] ?? 0;
              return (
                <li
                  key={z.id}
                  className="zone-item"
                  onClick={() => goZoneDetail(z)}
                  role="button"
                  title={`${z.label} 남은 좌석 ${remain}석`}
                >
                  <span className="zone-label">{z.label}</span>
                  <span className="zone-count">
                    {loadingMap ? "…" : remain}
                    <span className="unit">석</span>
                  </span>
                </li>
              );
            })}
          </ul>
        </div>
      </aside>
    </div>
  );
}
