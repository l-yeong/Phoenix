import React, { useRef, useState, useEffect } from "react";

/**
 * ZoomableSvgOverlay (반응형 버전)
 *
 * 역할:
 *  - 배경 이미지 위에 비율 기반 폴리곤을 그리고
 *  - 마우스/터치로 줌·패닝·리셋 기능 제공
 *  - 부모에서 전달한 onZoneClick / onZoneHover 등 이벤트 반영
 *
 * 변경점:
 *  - w-full + aspect-video 적용 (반응형)
 *  - 모바일 pinch-zoom 및 터치 드래그 지원
 *  - 툴팁 등 화면 경계 보정
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

  // ───────────────────────────────────────────────
  // 1️⃣ 줌 (마우스 휠)
  // ───────────────────────────────────────────────
  const onWheel = (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -0.1 : 0.1;
    setZoom((z) => Math.min(4, Math.max(0.6, z + delta)));
  };

  // ───────────────────────────────────────────────
  // 2️⃣ 패닝 (마우스 드래그)
  // ───────────────────────────────────────────────
  const clientToSvgRatio = () => {
    const el = wrapperRef.current;
    if (!el) return 1;
    const rect = el.getBoundingClientRect();
    return 100 / rect.width;
  };

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

  // ───────────────────────────────────────────────
  // 3️⃣ 터치 이벤트 (모바일 pinch / drag)
  // ───────────────────────────────────────────────
  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;

    let lastDist = null;
    let lastTouch = null;

    const getDist = (t1, t2) => Math.hypot(t2.clientX - t1.clientX, t2.clientY - t1.clientY);

    const onTouchMove = (e) => {
      if (e.touches.length === 2) {
        // pinch zoom
        const dist = getDist(e.touches[0], e.touches[1]);
        if (lastDist) {
          const delta = dist - lastDist;
          setZoom((z) => Math.min(4, Math.max(0.6, z + delta * 0.002)));
        }
        lastDist = dist;
      } else if (e.touches.length === 1 && lastTouch) {
        // drag
        const ratio = clientToSvgRatio();
        const dx = e.touches[0].clientX - lastTouch.x;
        const dy = e.touches[0].clientY - lastTouch.y;
        setTx((prev) => prev + dx * ratio);
        setTy((prev) => prev + dy * ratio);
        lastTouch = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      }
    };

    const onTouchStart = (e) => {
      if (e.touches.length === 1)
        lastTouch = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      else if (e.touches.length === 2) lastDist = getDist(e.touches[0], e.touches[1]);
    };

    const onTouchEnd = () => {
      lastTouch = null;
      lastDist = null;
    };

    el.addEventListener("touchstart", onTouchStart);
    el.addEventListener("touchmove", onTouchMove);
    el.addEventListener("touchend", onTouchEnd);

    return () => {
      el.removeEventListener("touchstart", onTouchStart);
      el.removeEventListener("touchmove", onTouchMove);
      el.removeEventListener("touchend", onTouchEnd);
    };
  }, []);

  // ───────────────────────────────────────────────
  // 4️⃣ 렌더링
  // ───────────────────────────────────────────────
  return (
    <div
      ref={wrapperRef}
      onWheel={onWheel}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={endPan}
      onMouseLeave={() => {
        endPan();
        onZoneLeave?.();
      }}
      className="relative w-full max-w-6xl aspect-video border rounded-md overflow-hidden select-none bg-gray-50"
      style={{ touchAction: "none", margin: "0 auto" }}
    >
      {/* 배경 이미지 */}
      <img
        src={backgroundUrl}
        alt="seats"
        className="absolute inset-0 w-full h-full object-cover pointer-events-none"
        draggable={false}
      />

      {/* SVG 오버레이 */}
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
                  onClick={(e) => {
                    e.stopPropagation();
                    onZoneClick?.(z);
                  }}
                  onMouseEnter={(e) =>
                    onZoneHover?.(z, {
                      clientX: Math.min(window.innerWidth - 120, e.clientX + 10),
                      clientY: e.clientY + 10,
                    })
                  }
                  onMouseMove={(e) =>
                    onZoneHover?.(z, {
                      clientX: Math.min(window.innerWidth - 120, e.clientX + 10),
                      clientY: e.clientY + 10,
                    })
                  }
                  onMouseLeave={() => onZoneLeave?.()}
                />
                {badge && (
                  <text
                    x={z.points_pct.reduce((a, b) => a + b.xPct, 0) / z.points_pct.length}
                    y={z.points_pct.reduce((a, b) => a + b.yPct, 0) / z.points_pct.length}
                    textAnchor="middle"
                    dominantBaseline="middle"
                    fontSize="3"
                    fill="#111"
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

      {/* 하단 컨트롤 버튼 */}
      <div className="absolute right-3 bottom-3 flex flex-col gap-2">
        <button
          className="bg-white/80 border rounded px-2 py-1 shadow hover:bg-white"
          onClick={() => setZoom((z) => Math.min(4, z + 0.2))}
        >
          +
        </button>
        <button
          className="bg-white/80 border rounded px-2 py-1 shadow hover:bg-white"
          onClick={() => setZoom((z) => Math.max(0.6, z - 0.2))}
        >
          –
        </button>
        <button
          className="bg-white/80 border rounded px-2 py-1 shadow hover:bg-white"
          onClick={() => {
            setZoom(1);
            setTx(0);
            setTy(0);
          }}
        >
          reset
        </button>
      </div>
    </div>
  );
}
