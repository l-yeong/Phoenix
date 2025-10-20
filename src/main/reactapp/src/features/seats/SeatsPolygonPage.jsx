import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import ZoomableSvgOverlay from "../../components/ZoomableSvgOverlay";
import rawZones from "../../data/zones.json";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

/** 데모용: 존별 총 좌석 수 추정 */
const DEMO_TOTAL_BY_ZONE = {
  centerZone: 80,
  gyeoreZone: 50,
  yeonwooZone: 60,
  seonghoZone: 40,
  chanyeongZone: 35,
  outfieldZone: 120,
};

export default function SeatsPolygonPage() {
  const navigate = useNavigate();

  // ✅ Gate/Macro에서 넘겨받은 uno, gno, token
  const { state } = useLocation(); // { uno, gno, token }
  const uno = state?.uno;
  const gno = state?.gno;
  const captchaToken = state?.token; // "local-captcha-ok"

  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ────────────────────────────────
  // 1) 게이트/캡차 검증 가드
  // ────────────────────────────────
  useEffect(() => {
    if (!uno || !gno || !captchaToken) navigate("/gate");
  }, [uno, gno, captchaToken, navigate]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(uno || "")}`);
        if (!cancelled && !data?.ready) navigate("/gate");
      } catch {
        if (!cancelled) navigate("/gate");
      }
    })();
    return () => { cancelled = true; };
  }, [api, uno, navigate]);

  // ────────────────────────────────
  // 2) 폴리곤 데이터 로드
  // ────────────────────────────────
  const zones = useMemo(
    () => rawZones.map((z) => ({ id: z.id, label: z.label, points_pct: z.points_pct })),
    []
  );

  // ────────────────────────────────
  // 3) 데모 잔여 좌석 수 상태
  // ────────────────────────────────
  const [remainByZone, setRemainByZone] = useState(() => {
    const obj = {};
    zones.forEach((z) => {
      const total = DEMO_TOTAL_BY_ZONE[z.id] ?? 50;
      const soldGuess = Math.floor(Math.random() * (total * 0.5)); // 0~50% 매진 가정
      obj[z.id] = total - soldGuess;
    });
    return obj;
  });

  const refreshDemo = () => {
    const obj = {};
    zones.forEach((z) => {
      const total = DEMO_TOTAL_BY_ZONE[z.id] ?? 50;
      const soldGuess = Math.floor(Math.random() * (total * 0.5));
      obj[z.id] = total - soldGuess;
    });
    setRemainByZone(obj);
  };

  // ────────────────────────────────
  // 4) Hover 툴팁 상태
  // ────────────────────────────────
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

  // ────────────────────────────────
  // 5) 존 클릭 시 상세 페이지로 이동
  // ────────────────────────────────
  const goZone = (zone) => {
    navigate(`/zone/${zone.id}`, { state: { uno, gno, token: captchaToken } });
  };

  // ────────────────────────────────
  // 6) 렌더링
  // ────────────────────────────────
  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-2xl font-bold">🎟️ 좌석 선택</h2>
        <div className="flex items-center gap-2">
          <button
            onClick={refreshDemo}
            className="border rounded px-3 py-1 hover:bg-gray-100 transition"
          >
            새로고침
          </button>
        </div>
      </div>

      {/* 줌/드래그 가능한 좌석 지도 */}
      <ZoomableSvgOverlay
        backgroundUrl="/stadium.png"     // public 경로
        zones={zones}                     // JSON에서 가져온 존 정보
        selected={new Set()}              // 이 페이지는 선택 상태 없음
        onZoneClick={goZone}              // 클릭 시 상세 페이지 이동
        onZoneHover={handleZoneHover}     // 툴팁 표시
        onZoneLeave={handleZoneLeave}
      />

      {/* Hover 툴팁 */}
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
        • 이 화면은 <b>존 단위 데모</b>입니다. 각 존을 클릭하면 상세 좌석 선택 페이지로 이동합니다. <br />
        • 잔여 좌석 수는 임시 계산이며, 실제 서비스에서는 <b>/seat/map</b> API 결과를 반영하세요.
      </div>
    </div>
  );
}
