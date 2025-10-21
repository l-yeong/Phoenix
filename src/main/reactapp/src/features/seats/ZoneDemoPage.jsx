import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import zonesData from "../../data/zones.json";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const ROWS = 10;
const COLS = 10;
const DEMO_SOLD_RATIO = 0.25;

const COLOR = {
  SOLD: "bg-gray-300 text-gray-600",
  AVAILABLE: "bg-green-400 hover:bg-green-500 text-white",
  HELD_BY_ME: "bg-blue-500 hover:bg-blue-600 text-white",
  SELECTED: "bg-blue-600 text-white",
  SENIOR: "border-2 border-pink-400 hover:bg-pink-100 text-gray-800"
};

export default function ZoneDemoPage() {
  const navigate = useNavigate();
  const { state } = useLocation();

  // ✅ uno, gno, captchaToken 받아오기
  const uno = state?.uno;
  const gno = state?.gno;
  const captchaToken = state?.token;

  const { zoneId } = useParams();
  const zoneMeta = useMemo(() => zonesData.find((z) => z.id === zoneId), [zoneId]);
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ✅ 게이트 인증 가드
  useEffect(() => {
    if (!uno || !gno || !captchaToken) navigate("/gate");
  }, [uno, gno, captchaToken, navigate]);

  // ✅ 세션 만료 대비 (게이트 재검증)
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

  // ✅ 초기 좌석 상태 (데모)
  const [statusMap, setStatusMap] = useState(() => {
    const map = {};
    for (let r = 1; r <= ROWS; r++) {
      for (let c = 1; c <= COLS; c++) {
        const id = `${zoneId}-${r}${c}`;
        const sold = Math.random() < DEMO_SOLD_RATIO;
        map[id] = sold ? "SOLD" : "AVAILABLE";
      }
    }
    return map;
  });

  const [selected, setSelected] = useState(new Set());

  // ✅ 새로고침 (데모 리셋)
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

  // ✅ 좌석 선택/해제
  const toggleSeat = async (seatId) => {
    const status = statusMap[seatId];
    if (status === "SOLD") return;

    const isMine = selected.has(seatId);

    if (isMine) {
      // 해제 요청
      try {
        const { data } = await api.post("/seat/release", { uno, gno, seatId });
        if (data?.ok) {
          const next = new Set(selected);
          next.delete(seatId);
          setSelected(next);
          setStatusMap((prev) => ({ ...prev, [seatId]: "AVAILABLE" }));
        } else alert("해제 실패");
      } catch {
        alert("네트워크 오류로 해제 실패");
      }
    } else {
      // 선택 요청
      try {
        const { data } = await api.post("/seat/select", { uno, gno, seatId });
        if (data?.ok) {
          const next = new Set(selected);
          next.add(seatId);
          setSelected(next);
          setStatusMap((prev) => ({ ...prev, [seatId]: "HELD_BY_ME" }));
        } else {
          const msg =
            data?.code === -1 ? "세션이 없습니다." :
            data?.code === -2 ? "이미 예매한 사용자입니다." :
            data?.code === -3 ? "이미 홀드/매진된 좌석입니다." :
            data?.code === -4 ? "좌석 한도(4개)를 초과했습니다." :
            "선택 실패";
          alert(msg);
        }
      } catch {
        alert("네트워크 오류로 선택 실패");
      }
    }
  };

  // ✅ 툴팁
  const [hoverInfo, setHoverInfo] = useState(null);
  const handleMouseEnter = (seatId, e) => setHoverInfo({ seatId, status: statusMap[seatId], x: e.clientX, y: e.clientY });
  const handleMouseMove = (seatId, e) => setHoverInfo((p) => (p ? { ...p, x: e.clientX, y: e.clientY } : null));
  const handleMouseLeave = () => setHoverInfo(null);

  if (!zoneMeta) {
    return (
      <div className="max-w-3xl mx-auto p-6">
        <p className="text-lg font-semibold">존 정보를 찾을 수 없습니다.</p>
        <button className="mt-3 border rounded px-3 py-1" onClick={() => navigate("/seats")}>
          좌석 지도로 돌아가기
        </button>
      </div>
    );
  }

  const zoneLabel = zoneMeta?.label || zoneId;
  const remainCount = Object.values(statusMap).filter((s) => s === "AVAILABLE").length;

  return (
    <div className="max-w-5xl mx-auto p-8 bg-gray-50 rounded-lg shadow-md">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-3xl font-bold text-gray-800">{zoneLabel}</h2>
          <p className="text-sm text-gray-500">가용 좌석: {remainCount}석</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => navigate("/seats", { state })}
            className="px-3 py-1 border rounded hover:bg-gray-100"
          >
            ← 좌석 지도
          </button>
          <button onClick={refreshDemo} className="px-3 py-1 border rounded hover:bg-gray-100">
            새로고침
          </button>
        </div>
      </div>

      {/* 범례 */}
      <div className="flex gap-4 text-sm mb-5">
        <Legend color="bg-green-400" label="선택 가능" />
        <Legend color="bg-blue-500" label="내 좌석(임시)" />
        <Legend color="bg-gray-300" label="매진" />
        {zoneId === "A" && <Legend color="border-2 border-pink-400" label="노인석" />}
      </div>

      {/* 좌석 그리드 */}
      <div
        className="inline-grid bg-white p-4 rounded-lg shadow"
        style={{ gridTemplateColumns: `repeat(${COLS}, 2.2rem)`, gap: "0.4rem" }}
      >
        {Array.from({ length: ROWS }).map((_, ri) =>
          Array.from({ length: COLS }).map((__, ci) => {
            const seatId = `${zoneId}-${ri + 1}${ci + 1}`;
            const status = statusMap[seatId];
            const isMine = selected.has(seatId);
            const cls =
              status === "SOLD"
                ? COLOR.SOLD
                : isMine
                ? COLOR.SELECTED
                : status === "HELD_BY_ME"
                ? COLOR.HELD_BY_ME
                : zoneId === "A"
                ? COLOR.SENIOR
                : COLOR.AVAILABLE;

            return (
              <button
                key={seatId}
                onClick={() => toggleSeat(seatId)}
                onMouseEnter={(e) => handleMouseEnter(seatId, e)}
                onMouseMove={(e) => handleMouseMove(seatId, e)}
                onMouseLeave={handleMouseLeave}
                className={`w-9 h-9 text-[10px] font-semibold rounded-md ${cls} transition`}
                disabled={status === "SOLD"}
              >
                {ri + 1}-{ci + 1}
              </button>
            );
          })
        )}
      </div>

      {/* 선택 목록 */}
      <div className="mt-5 p-4 border rounded bg-white">
        <h3 className="font-semibold mb-2">선택 좌석</h3>
        {selected.size === 0 ? (
          <p className="text-sm text-gray-600">선택된 좌석이 없습니다.</p>
        ) : (
          <ul className="list-disc text-sm pl-5">
            {Array.from(selected).map((sid) => (
              <li key={sid}>{sid}</li>
            ))}
          </ul>
        )}
      </div>

      {/* 툴팁 */}
      {hoverInfo && (
        <div
          className="fixed z-50 bg-black text-white text-xs px-2 py-1 rounded shadow-lg"
          style={{ top: hoverInfo.y + 10, left: hoverInfo.x + 10 }}
        >
          <div className="font-semibold">{hoverInfo.seatId}</div>
          <div>상태: {translateStatus(hoverInfo.status)}</div>
        </div>
      )}
    </div>
  );
}


function Legend({ color, label }) {
  return (
    <div className="flex items-center gap-2">
      <span className={`inline-block w-4 h-4 rounded ${color}`} />
      <span>{label}</span>
    </div>
  );
}

function translateStatus(s) {
  if (s === "AVAILABLE") return "선택 가능";
  if (s === "HELD_BY_ME") return "내 좌석";
  if (s === "SOLD") return "매진";
  return s;
}
