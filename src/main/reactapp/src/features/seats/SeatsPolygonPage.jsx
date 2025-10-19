import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import ZoomableSvgOverlay from "../../components/ZoomableSvgOverlay";
import rawZones from "../../data/zones.json";

/**
 * SeatsPolygonPage
 *
 * 역할
 *  - 게이트 세션 ready + 캡차 통과 여부를 검증(가드)
 *  - 배경 이미지 위에 상대좌표(%) 폴리곤을 그리고, 줌/드래그/리셋을 제공
 *  - Hover 시 툴팁으로 "남은 좌석 n석" 표시 (데모 로직)
 *  - 존 클릭 시 ZoneDemoPage(`/zone/:zoneId`)로 이동 (state 유지)
 *
 * 주의
 *  - 실제 잔여 좌석/상태는 서버 API(/seat/map 등)로 교체하면 됨. 여기선 데모 계산만 수행.
 */

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

  // Gate/Macro에서 넘겨준 값 (반드시 있어야 함)
  const { state } = useLocation();           // { userId, showId, token }
  const userId = state?.userId;
  const showId = state?.showId;
  const captchaToken = state?.token;         // "local-captcha-ok" 또는 reCAPTCHA 토큰 등

  // 페이지 전용 axios 인스턴스
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ──────────────────────────────────────────────────────────────
  // 1) 가드: 필수 파라미터 없으면 게이트로 회귀
  // ──────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!userId || !showId || !captchaToken) navigate("/gate");
  }, [userId, showId, captchaToken, navigate]);

  // 2) 가드: 게이트 세션 ready 확인(중간 만료 대비)
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const { data } = await api.get(`/gate/check/${encodeURIComponent(userId || "")}`);
        if (!cancelled && !data?.ready) navigate("/gate");
      } catch {
        if (!cancelled) navigate("/gate");
      }
    })();
    return () => { cancelled = true; };
  }, [api, userId, navigate]);

  // ──────────────────────────────────────────────────────────────
  // 3) 폴리곤 데이터 준비 (zones.json → id/label/points_pct)
  // ──────────────────────────────────────────────────────────────
  const zones = useMemo(
    () => rawZones.map((z) => ({ id: z.id, label: z.label, points_pct: z.points_pct })),
    []
  );

  // ──────────────────────────────────────────────────────────────
  // 4) 데모 잔여 좌석 수 상태
  //   - 실제에선 서버가 SOLD/HELD 수를 보내주면 정확함
  //   - 여기선 "총좌석 - 랜덤 판매분"으로 데모 값을 만든 뒤 유지
  // ──────────────────────────────────────────────────────────────
  const [remainByZone, setRemainByZone] = useState(() => {
    const obj = {};
    zones.forEach((z) => {
      const total = DEMO_TOTAL_BY_ZONE[z.id] ?? 50;
      const soldGuess = Math.floor(Math.random() * Math.min(total * 0.5, total)); // 0~50% 이미 팔렸다고 가정
      obj[z.id] = Math.max(total - soldGuess, 0);
    });
    return obj;
  });

  // "새로고침" → 데모 상태 리셋
  const refreshDemo = () => {
    const obj = {};
    zones.forEach((z) => {
      const total = DEMO_TOTAL_BY_ZONE[z.id] ?? 50;
      const soldGuess = Math.floor(Math.random() * Math.min(total * 0.5, total));
      obj[z.id] = Math.max(total - soldGuess, 0);
    });
    setRemainByZone(obj);
  };

  // ──────────────────────────────────────────────────────────────
  // 5) Hover 툴팁
  // ──────────────────────────────────────────────────────────────
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

  // ──────────────────────────────────────────────────────────────
  // 6) 존 클릭 → ZoneDemoPage로 이동
  //   - userId/showId/token state를 그대로 넘겨 가드 통과 보장
  // ──────────────────────────────────────────────────────────────
  const goZone = (zone) => {
    navigate(`/zone/${zone.id}`, { state: { userId, showId, token: captchaToken } });
  };

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* 상단 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-2xl font-bold">🎟️ 좌석 선택</h2>
        <div className="flex items-center gap-2">
          <button onClick={refreshDemo} className="border rounded px-3 py-1">
            새로고침
          </button>
        </div>
      </div>

      {/* 줌/드래그 가능한 지도 + 폴리곤 오버레이 */}
      <ZoomableSvgOverlay
        backgroundUrl="/stadium.png"        // public/stadium.png
        zones={zones}                        // 상대좌표 points_pct 사용
        selected={new Set()}                 // 이 페이지는 존 선택 상태를 유지하지 않음(ZoneDemo에서 선택)
        onZoneClick={goZone}                 // 클릭하면 상세로 이동
        onZoneHover={handleZoneHover}        // 툴팁 표시
        onZoneLeave={handleZoneLeave}
      />

      {/* Hover 툴팁: "남은 좌석 n석" */}
      {tooltip && (
        <div
          className="fixed z-50 bg-black text-white text-xs px-2 py-1 rounded"
          style={{ top: tooltip.y + 12, left: tooltip.x + 12 }}
        >
          <div className="font-semibold">{tooltip.label}</div>
          <div>남은 좌석: {tooltip.remain}석</div>
        </div>
      )}

      {/* 안내 영역 (옵션) */}
      <div className="mt-4 text-xs text-gray-500 leading-5">
        • 현재 화면은 <b>존 단위 데모</b>입니다. 각 존을 클릭하면 상세 좌석(개별 선택) 페이지로 이동합니다. <br />
        • 잔여 좌석 수는 데모 계산이며, 실제 서비스에서는 서버 기준(/seat/map 등)으로 갱신하세요.
      </div>
    </div>
  );
}
