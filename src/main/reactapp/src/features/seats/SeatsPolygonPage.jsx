// src/pages/seats/SeatsPolygonPage.jsx
import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import ZoomableSvgOverlay from "../../components/ZoomableSvgOverlay";
import rawZones from "../../data/zones.json";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export default function SeatsPolygonPage() {
  const navigate = useNavigate();
  const { state } = useLocation(); // Gate/Macro에서 넘겨받은 { mno, gno, token }
  const mno = state?.mno;
  const gno = state?.gno;
  const captchaToken = state?.token;

  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // 1) 게이트/캡차 검증 가드
  useEffect(() => {
    if (!mno || !gno || !captchaToken) {
      navigate("/gate", { replace: true });
      return;
    }
    (async () => {
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(mno)}`);
        if (!data?.ready) navigate("/gate", { replace: true });
      } catch {
        navigate("/gate", { replace: true });
      }
    })();
  }, [api, mno, gno, captchaToken, navigate]);

  // 2) 폴리곤(존) 메타
  const zones = useMemo(
    () => rawZones.map((z) => ({ id: z.id, label: z.label, points_pct: z.points_pct })),
    []
  );

  // 3) 좌석 맵 실데이터: { [sno]: "AVAILABLE" | "HELD" | "HELD_BY_ME" | "SOLD" }
  const [seatMap, setSeatMap] = useState({});
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");

  const loadSeatMap = async () => {
    setLoading(true);
    setErr("");
    try {
      const { data } = await api.get("/seats/map", { params: { gno, mno } });
      setSeatMap(data || {});
    } catch (e) {
      setErr("좌석 지도를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (mno && gno) loadSeatMap();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mno, gno]);

  // 4) 존별 남은 좌석 계산 (AVAILABLE만 카운트)
  const remainByZone = useMemo(() => {
    const acc = {};
    for (const [sno, st] of Object.entries(seatMap)) {
      // sno 네이밍: "{zoneId}-{...}" 라는 전제
      const zoneId = sno.split("-")[0] || "unknownZone";

      // 👉 HELD_BY_ME까지 “남은 좌석”으로 보고 싶다면 아래 라인 조건을 바꿔라:
      // if (st === "AVAILABLE" || st === "HELD_BY_ME") ...
      if (st === "AVAILABLE") {
        acc[zoneId] = (acc[zoneId] || 0) + 1;
      }
    }
    return acc;
  }, [seatMap]);

  // 5) Hover 툴팁
  const [tooltip, setTooltip] = useState(null); // { label, remain, x, y }
  const handleZoneHover = (zone, { clientX, clientY }) => {
    setTooltip({
      label: zone.label,
      remain: remainByZone[zone.id] ?? 0,
      x: clientX,
      y: clientY,
    });
  };
  const handleZoneLeave = () => setTooltip(null);

  // 6) 존 클릭 → 존 상세(그리드) 페이지로 이동
  const goZone = (zone) => {
    navigate(`/zone/${zone.id}`, { state: { mno, gno, token: captchaToken } });
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-2xl font-bold">🎟️ 좌석 선택</h2>
        <div className="flex items-center gap-2">
          <button
            onClick={loadSeatMap}
            className="border rounded px-3 py-1 hover:bg-gray-100 transition"
            disabled={loading}
          >
            {loading ? "불러오는 중…" : "새로고침"}
          </button>
        </div>
      </div>

      {/* 로딩/에러 */}
      {err && <div className="text-sm text-red-600 mb-2">{err}</div>}

      {/* 줌/드래그 가능한 좌석 지도 */}
      <ZoomableSvgOverlay
        backgroundUrl="/stadium.png"     // public 경로
        zones={zones}                     // JSON에서 가져온 존 정보
        selected={new Set()}              // 이 페이지는 선택 상태 없음
        onZoneClick={goZone}              // 클릭 시 상세 페이지 이동
        onZoneHover={handleZoneHover}     // 툴팁 표시
        onZoneLeave={handleZoneLeave}
      />

      {/* Hover 툴팁: 실데이터 기반 남은 좌석 표시 */}
      {tooltip && (
        <div
          className="fixed z-50 bg-black text-white text-xs px-2 py-1 rounded"
          style={{ top: tooltip.y + 12, left: tooltip.x + 12 }}
        >
          <div className="font-semibold">{tooltip.label}</div>
          <div>남은 좌석: {tooltip.remain}석</div>
        </div>
      )}

      {/* 안내 문구 */}
      <div className="mt-4 text-xs text-gray-500 leading-5">
        • “남은 좌석”은 현재 서버가 반환한 <b>AVAILABLE</b> 좌석 수 기준입니다. <br />
        • 필요하면 <code>remainByZone</code> 계산에서 <b>HELD_BY_ME</b>도 포함하도록 수정하세요. <br />
        • 존을 클릭하면 해당 존의 상세 좌석 선택 페이지로 이동합니다.
      </div>
    </div>
  );
}
