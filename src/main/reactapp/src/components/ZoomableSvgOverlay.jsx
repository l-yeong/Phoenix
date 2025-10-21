import React, { useEffect, useRef, useState, useMemo } from "react";

/**
 * zones: [{ id, label, zno?, points_pct: [{xPct, yPct}, ...] }]
 * props:
 *  - backgroundUrl
 *  - zones, onZoneClick, onZoneHover, onZoneLeave
 *  - minZoom=0.75, maxZoom=4, initialZoom=1
 */
export default function ZoomableSvgOverlay({
  backgroundUrl,
  zones = [],
  onZoneClick,
  onZoneHover,
  onZoneLeave,
  minZoom = 0.75,
  maxZoom = 4,
  initialZoom = 1,
}) {
  const [nat, setNat] = useState({ w: 100, h: 100 });
  const svgRef = useRef(null);

  // 줌/이동 상태
  const [state, setState] = useState({ s: initialZoom, tx: 0, ty: 0 });

  // 드래그 제스처 관리
  const gesture = useRef({
    active: false,        // 실제 팬 중인지
    pointerId: null,
    startX: 0,
    startY: 0,
    lastX: 0,
    lastY: 0,
    moved: false,         // 클릭 vs 드래그 구분
  });
  const DRAG_THRESHOLD = 3; // 3px 이하면 클릭으로 간주

  // 원본 크기 로드
  useEffect(() => {
    if (!backgroundUrl) return;
    const img = new Image();
    img.onload = () => setNat({ w: img.naturalWidth || 100, h: img.naturalHeight || 100 });
    img.src = backgroundUrl;
  }, [backgroundUrl]);

  const aspect = useMemo(() => (nat.h ? nat.w / nat.h : 1), [nat.w, nat.h]);

  // 퍼센트 → 픽셀
  const toPoints = (pts = []) =>
    pts.map(({ xPct, yPct }) => `${(xPct / 100) * nat.w},${(yPct / 100) * nat.h}`).join(" ");

  // 화면좌표 → SVG 좌표
  const clientToSvgPoint = (clientX, clientY) => {
    const svg = svgRef.current;
    if (!svg) return { x: 0, y: 0 };
    const pt = svg.createSVGPoint();
    pt.x = clientX;
    pt.y = clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return { x: 0, y: 0 };
    const inv = ctm.inverse();
    const sp = pt.matrixTransform(inv);
    const { s, tx, ty } = state;
    return { x: (sp.x - tx) / s, y: (sp.y - ty) / s };
  };

  // Wheel 줌 (커서 기준)
  const handleWheel = (e) => {
    e.preventDefault();
    const delta = Math.sign(e.deltaY);
    const zoomFactor = delta > 0 ? 0.9 : 1.1;

    const { s, tx, ty } = state;
    let newScale = Math.max(minZoom, Math.min(maxZoom, s * zoomFactor));
    if (newScale === s) return;

    const svgP = clientToSvgPoint(e.clientX, e.clientY);
    const newTx = tx + svgP.x * (s - newScale);
    const newTy = ty + svgP.y * (s - newScale);

    setState({ s: newScale, tx: newTx, ty: newTy });
  };

  // Pointer (팬)
  const onPointerDown = (e) => {
    if (e.button !== 0) return;
    gesture.current = {
      active: false,           // 아직 팬 시작 안 함
      pointerId: e.pointerId,
      startX: e.clientX,
      startY: e.clientY,
      lastX: e.clientX,
      lastY: e.clientY,
      moved: false,
    };
    // ⚠ 캡처는 바로 하지 않음 (클릭 유지)
  };

  const onPointerMove = (e) => {
    const g = gesture.current;
    if (g.pointerId !== e.pointerId) return;

    const dx = e.clientX - g.lastX;
    const dy = e.clientY - g.lastY;
    g.lastX = e.clientX;
    g.lastY = e.clientY;

    // 아직 팬 시작 전: 임계치 넘으면 팬 시작 + 포인터 캡처
    if (!g.active) {
      const totalDx = e.clientX - g.startX;
      const totalDy = e.clientY - g.startY;
      if (Math.hypot(totalDx, totalDy) > DRAG_THRESHOLD) {
        g.active = true;
        g.moved = true;
        svgRef.current?.setPointerCapture?.(g.pointerId);
      } else {
        return; // 클릭 임계 전에는 아무 것도 안 함
      }
    }

    // 팬 중이면 이동
    setState((st) => ({ ...st, tx: st.tx + dx, ty: st.ty + dy }));
  };

  const onPointerUp = (e) => {
    const g = gesture.current;
    if (g.pointerId !== e.pointerId) return;
    // 캡처 해제
    svgRef.current?.releasePointerCapture?.(g.pointerId);
    // 팬이 아니고 moved=false면 클릭으로 간주 → polygon onClick이 정상 동작
    gesture.current = { active: false, pointerId: null, startX: 0, startY: 0, lastX: 0, lastY: 0, moved: false };
  };

  const onDoubleClick = () => setState({ s: initialZoom, tx: 0, ty: 0 });

  return (
    <div className="relative mx-auto" style={{ width: "100%", maxWidth: "1200px", aspectRatio: aspect }}>
      <svg
        ref={svgRef}
        viewBox={`0 0 ${nat.w} ${nat.h}`}
        preserveAspectRatio="xMidYMid meet"
        className="absolute inset-0"
        style={{ width: "100%", height: "100%", display: "block", touchAction: "none" }}
        onWheel={handleWheel}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onDoubleClick={onDoubleClick}
      >
        <g transform={`translate(${state.tx} ${state.ty}) scale(${state.s})`}>
          <image
            href={backgroundUrl}
            x="0"
            y="0"
            width={nat.w}
            height={nat.h}
            preserveAspectRatio="xMidYMid meet"
            style={{ pointerEvents: "none", userSelect: "none" }}
          />
          {zones.map((z) => (
            <polygon
              key={z.id}
              points={toPoints(z.points_pct)}
              onClick={(e) => onZoneClick?.(z, e)}       // ✅ 클릭 통과
              onMouseMove={(e) => onZoneHover?.(z, e)}
              onMouseLeave={() => onZoneLeave?.()}
              style={{
                fill: "rgba(255,255,255,0.22)",
                stroke: "rgba(0,0,0,0.35)",
                strokeWidth: Math.max(nat.w, nat.h) * 0.002,
                cursor: "pointer",
                transition: "fill .12s ease",
              }}
              onMouseEnter={(e) => (e.currentTarget.style.fill = "rgba(255,255,255,0.36)")}
              onMouseOut={(e) => (e.currentTarget.style.fill = "rgba(255,255,255,0.22)")}
            />
          ))}
        </g>
      </svg>
    </div>
  );
}
