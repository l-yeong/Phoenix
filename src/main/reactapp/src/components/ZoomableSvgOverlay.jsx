import React, { useRef, useState } from "react";

/**
 * ZoomableSvgOverlay
 *
 * 역할
 *  - 배경 이미지 위에 상대좌표(%) 폴리곤을 오버레이로 그리고
 *  - 휠/드래그로 줌/패닝을 제공
 *  - 폴리곤 클릭/호버 이벤트를 부모로 전달
 *
 * props
 *  - backgroundUrl: 배경 이미지 경로 (예: /stadium.png)
 *  - zones: [{ id, label, points_pct: [{ xPct, yPct }, ...] }]
 *  - selected: Set<string>  선택된 zone id 모음
 *  - onZoneClick(zone)
 *  - onZoneHover(zone, {clientX, clientY})   // 툴팁 표시에 사용
 *  - onZoneLeave()
 *  - badge: 라벨 텍스트 표시 여부
 */
export default function ZoomableSvgOverlay({
  backgroundUrl,
  zones,
  selected = new Set(),
  onZoneClick,
  onZoneHover,
  onZoneLeave,
  badge = true,
}) {
  const wrapperRef = useRef(null);
  const [zoom, setZoom] = useState(1);
  const [tx, setTx] = useState(0);
  const [ty, setTy] = useState(0);
  const [panning, setPanning] = useState(false);
  const [start, setStart] = useState({ x: 0, y: 0 });

  // 마우스 휠 줌(0.6x ~ 4x)
  const onWheel = (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setZoom((z) => Math.min(4, Math.max(0.6, z + delta)));
  };

  // 픽셀 → viewBox(0~100) 환산비
  const clientToSvgRatio = () => {
    const el = wrapperRef.current;
    if (!el) return 1;
    const rect = el.getBoundingClientRect();
    return 100 / rect.width;
  };

  // 패닝
  const onMouseDown = (e) => {
    setPanning(true);
    setStart({ x: e.clientX, y: e.clientY });
  };
  const onMouseMove = (e) => {
    if (!panning) return;
    const ratio = clientToSvgRatio();
    setTx((prev) => prev + (e.clientX - start.x) * ratio);
    setTy((prev) => prev + (e.clientY - start.y) * ratio);
    setStart({ x: e.clientX, y: e.clientY });
  };
  const endPan = () => setPanning(false);

  return (
    <div
      ref={wrapperRef}
      onWheel={onWheel}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={endPan}
      onMouseLeave={() => { endPan(); onZoneLeave?.(); }}
      className="relative w-[900px] h-[600px] border rounded-md overflow-hidden select-none"
      style={{ touchAction: "none" }}
    >
      {/* 배경 */}
      <img
        src={backgroundUrl}
        alt="seats"
        className="absolute inset-0 w-full h-full object-cover pointer-events-none"
        draggable={false}
      />

      {/* SVG 오버레이(viewBox=0~100) */}
      <svg className="absolute inset-0 w-full h-full" viewBox="0 0 100 100" preserveAspectRatio="none">
        <g transform={`translate(${tx}, ${ty}) scale(${zoom})`}>
          {zones.map((z) => {
            const points = z.points_pct.map((p) => `${p.xPct},${p.yPct}`).join(" ");
            const isSel = selected.has(z.id);
            return (
              <g key={z.id} style={{ cursor: "pointer" }}>
                <polygon
                  points={points}
                  fill={isSel ? "rgba(0,150,255,0.6)" : "rgba(255,0,0,0.28)"}
                  stroke={isSel ? "blue" : "red"}
                  strokeWidth="0.3"
                  onClick={(e) => { e.stopPropagation(); onZoneClick?.(z); }}
                  onMouseEnter={(e) => onZoneHover?.(z, { clientX: e.clientX, clientY: e.clientY })}
                  onMouseMove={(e) => onZoneHover?.(z, { clientX: e.clientX, clientY: e.clientY })}
                  onMouseLeave={() => onZoneLeave?.()}
                />
                {badge && (
                  <text
                    x={z.points_pct.reduce((a, b) => a + b.xPct, 0) / z.points_pct.length}
                    y={z.points_pct.reduce((a, b) => a + b.yPct, 0) / z.points_pct.length}
                    textAnchor="middle" dominantBaseline="middle"
                    fontSize="3" fill="#111"
                    style={{ userSelect: "none", pointerEvents: "none" }}
                  >
                    {z.label}
                  </text>
                )}
              </g>
            );
          })}
        </g>
      </svg>

      {/* 우하단 줌 컨트롤 */}
      <div className="absolute right-3 bottom-3 flex flex-col gap-2">
        <button className="bg-white/80 border rounded px-2 py-1"
                onClick={() => setZoom((z) => Math.min(4, z + 0.2))}>+</button>
        <button className="bg-white/80 border rounded px-2 py-1"
                onClick={() => setZoom((z) => Math.max(0.6, z - 0.2))}>–</button>
        <button className="bg-white/80 border rounded px-2 py-1"
                onClick={() => { setZoom(1); setTx(0); setTy(0); }}>reset</button>
      </div>
    </div>
  );
}
