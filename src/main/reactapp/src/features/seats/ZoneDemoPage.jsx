import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import zonesData from "../../data/zones.json";

/**
 * ZoneDemoPage
 *
 * 역할
 * - 특정 존(zoneId)의 "개별 좌석" 데모를 보여준다.
 * - 게이트 세션(ready) + 캡차 통과 여부를 가드한다.
 * - 데모 좌석 그리드(예: 10x10)를 그리고, 랜덤 매진/가용 상태를 만든다.
 * - 클릭 시 /seat/select, 재클릭 시 /seat/release 호출로 백엔드 연동 플로우를 검증한다.
 *
 * 주의
 * - 실제 좌석 배치/가격/등급 등은 데이터로 교체해야 한다.
 * - seatId는 데모로 `${zoneId}-${row}${col}` 패턴을 사용한다. (백엔드에 그대로 전달)
 * - /seat/map 을 열면 SOLD/HELD 실시간 상태를 서버 기준으로 반영 가능(여기선 데모 랜덤)
 */

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

// 데모 좌석 크기/행열
const ROWS = 10;
const COLS = 10;

// 데모 랜덤 SOLD 비율(0.0~0.6 정도 추천)
const DEMO_SOLD_RATIO = 0.25;

// 칼라 팔레트(간단)
const COLOR = {
  SOLD: "#cbd5e1",        // 회색(불가)
  AVAILABLE: "#10b981",   // 초록
  HELD_BY_ME: "#3b82f6",  // 파랑
  SELECTED: "#2563eb",    // 선택(=HELD_BY_ME와 동일 처럼 보여도 됨)
};

export default function ZoneDemoPage() {
  const navigate = useNavigate();
  const { state } = useLocation(); // { userId, showId, token } from previous page
  const userId = state?.userId;
  const showId = state?.showId;
  const captchaToken = state?.token;

  const { zoneId } = useParams();
  const zoneMeta = useMemo(() => zonesData.find((z) => z.id === zoneId), [zoneId]);

  // axios 인스턴스(페이지 단독)
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // 0) 가드: 파라미터 없으면 게이트로
  useEffect(() => {
    if (!userId || !showId || !captchaToken) {
      navigate("/gate");
    }
  }, [userId, showId, captchaToken, navigate]);

  // 1) 가드: 게이트 세션 ready 확인(중간 만료 대비)
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

  // 2) 데모 좌석 상태 만들기
  // statusMap: seatId -> "SOLD" | "AVAILABLE" | "HELD_BY_ME"
  const [statusMap, setStatusMap] = useState(() => {
    const map = {};
    for (let r = 1; r <= ROWS; r++) {
      for (let c = 1; c <= COLS; c++) {
        const id = `${zoneId}-${r}${c}`;
        // SOLD_RATIO 확률로 미리 매진
        const sold = Math.random() < DEMO_SOLD_RATIO;
        map[id] = sold ? "SOLD" : "AVAILABLE";
      }
    }
    return map;
  });

  // 3) 선택(나의 hold) 목록
  const [selected, setSelected] = useState(new Set());

  // 4) 새로고침(데모 리셋)
  const refreshDemo = () => {
    const map = {};
    for (let r = 1; r <= ROWS; r++) {
      for (let c = 1; c <= COLS; c++) {
        const id = `${zoneId}-${r}${c}`;
        const sold = Math.random() < DEMO_SOLD_RATIO;
        map[id] = sold ? "SOLD" : "AVAILABLE";
      }
    }
    setStatusMap(map);
    setSelected(new Set());
  };

  // 5) 좌석 클릭 핸들러
  const toggleSeat = async (seatId) => {
    const status = statusMap[seatId];
    if (status === "SOLD") return; // 불가

    const isMine = selected.has(seatId);
    if (isMine) {
      // 해제
      try {
        const { data } = await api.post("/seat/release", { userId, showId, seatId });
        if (data?.ok) {
          const nextSel = new Set(selected); nextSel.delete(seatId);
          setSelected(nextSel);
          setStatusMap((prev) => ({ ...prev, [seatId]: "AVAILABLE" }));
        } else {
          alert("해제 실패");
        }
      } catch {
        alert("네트워크 오류로 해제 실패");
      }
      return;
    }

    // 선택
    try {
      const { data } = await api.post("/seat/select", { userId, showId, seatId });
      if (data?.ok) {
        const nextSel = new Set(selected); nextSel.add(seatId);
        setSelected(nextSel);
        setStatusMap((prev) => ({ ...prev, [seatId]: "HELD_BY_ME" }));
      } else {
        // SeatLockService의 코드 정의에 맞춰 메시지 처리
        const code = data?.code;
        const msg =
          code === -1 ? "세션이 없습니다. 다시 입장해 주세요."
        : code === -2 ? "이미 해당 공연을 예매하셨습니다."
        : code === -3 ? "이미 홀드/매진된 좌석입니다."
        : code === -4 ? "보유 한도(4좌석)를 초과했습니다."
        : data?.message || "선택 실패";
        alert(msg);
      }
    } catch {
      alert("네트워크 오류로 선택 실패");
    }
  };

  // 6) 툴팁용 정보 (행/열 + 상태)
  const [hoverInfo, setHoverInfo] = useState(null); // { seatId, x, y, status }
  const handleMouseEnter = (seatId, e) => {
    setHoverInfo({ seatId, status: statusMap[seatId], x: e.clientX, y: e.clientY });
  };
  const handleMouseMove = (seatId, e) => {
    setHoverInfo((prev) => prev ? { ...prev, x: e.clientX, y: e.clientY } : null);
  };
  const handleMouseLeave = () => setHoverInfo(null);

  if (!zoneMeta) {
    return (
      <div className="max-w-3xl mx-auto p-6">
        <div className="text-lg font-semibold">존 정보를 찾을 수 없습니다.</div>
        <button className="mt-3 border rounded px-3 py-1" onClick={() => navigate("/seats")}>
          좌석 지도로 돌아가기
        </button>
      </div>
    );
  }

  // 존 레이블 (페이지 타이틀)
  const zoneLabel = zoneMeta?.label || zoneId;

  // 가용 좌석 수(데모 기준)
  const remainCount = Object.values(statusMap).filter((s) => s === "AVAILABLE").length;

  return (
    <div className="max-w-5xl mx-auto p-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-3">
        <div>
          <h2 className="text-2xl font-bold">{zoneLabel}</h2>
          <div className="text-sm text-gray-600">가용 좌석(데모): {remainCount}석</div>
        </div>
        <div className="flex items-center gap-2">
          <button className="border rounded px-3 py-1" onClick={() => navigate("/seats", { state })}>
            ← 좌석 지도
          </button>
          <button className="border rounded px-3 py-1" onClick={refreshDemo}>
            새로고침
          </button>
        </div>
      </div>

      {/* 범례 */}
      <div className="flex items-center gap-4 text-sm mb-3">
        <Legend color={COLOR.AVAILABLE} label="선택 가능" />
        <Legend color={COLOR.HELD_BY_ME} label="내가 보유(임시)" />
        <Legend color={COLOR.SOLD} label="매진" />
      </div>

      {/* 좌석 그리드 */}
      <div className="inline-grid" style={{ gridTemplateColumns: `repeat(${COLS}, 28px)`, gap: 8 }}>
        {Array.from({ length: ROWS }).map((_, ri) =>
          Array.from({ length: COLS }).map((__, ci) => {
            const seatId = `${zoneId}-${ri + 1}${ci + 1}`;
            const status = statusMap[seatId];
            const isMine = selected.has(seatId);
            const bg =
              status === "SOLD" ? COLOR.SOLD :
              isMine ? COLOR.SELECTED :
              status === "HELD_BY_ME" ? COLOR.HELD_BY_ME :
              COLOR.AVAILABLE;

            return (
              <button
                key={seatId}
                onClick={() => toggleSeat(seatId)}
                onMouseEnter={(e) => handleMouseEnter(seatId, e)}
                onMouseMove={(e) => handleMouseMove(seatId, e)}
                onMouseLeave={handleMouseLeave}
                className="w-7 h-7 rounded text-[10px] text-white grid place-items-center"
                style={{ background: bg, cursor: status === "SOLD" ? "not-allowed" : "pointer" }}
                disabled={status === "SOLD"}
                title={seatId}
              >
                {ri + 1}-{ci + 1}
              </button>
            );
          })
        )}
      </div>

      {/* 선택 정보 */}
      <div className="mt-4 border rounded p-3">
        <div className="font-semibold mb-2">선택 좌석</div>
        {selected.size === 0 ? (
          <div className="text-sm text-gray-600">선택된 좌석이 없습니다.</div>
        ) : (
          <ul className="text-sm list-disc pl-5">
            {Array.from(selected).map((sid) => <li key={sid}>{sid}</li>)}
          </ul>
        )}
        <div className="text-xs text-gray-500 mt-2">
          * 데모 페이지입니다. 실제 좌석 배치는 데이터로 교체하고, /seat/map 연동 시 SOLD/HELD 상태는 서버 기준으로 반영하세요.
        </div>
      </div>

      {/* 툴팁 */}
      {hoverInfo && (
        <div
          className="fixed z-50 bg-black text-white text-xs px-2 py-1 rounded"
          style={{ top: hoverInfo.y + 10, left: hoverInfo.x + 10 }}
        >
          <div className="font-semibold">{hoverInfo.seatId}</div>
          <div>상태: {translateStatus(hoverInfo.status)}</div>
        </div>
      )}
    </div>
  );
}

/** 작은 범례 뱃지 */
function Legend({ color, label }) {
  return (
    <div className="flex items-center gap-2">
      <span className="inline-block w-4 h-4 rounded" style={{ background: color }} />
      <span>{label}</span>
    </div>
  );
}

/** 상태 텍스트 한글화(툴팁용) */
function translateStatus(s) {
  if (s === "AVAILABLE") return "선택 가능";
  if (s === "HELD_BY_ME") return "내가 보유";
  if (s === "SOLD") return "매진";
  return s;
}
