// src/pages/seats/SeatsMapPage.jsx
import React, { useEffect, useMemo, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const COLOR = {
  AVAILABLE: "bg-green-500 hover:bg-green-600 text-white",
  HELD_BY_ME: "bg-blue-500 hover:bg-blue-600 text-white",
  HELD: "bg-gray-300 text-gray-600 cursor-not-allowed",
  SOLD: "bg-gray-400 text-gray-700 cursor-not-allowed",
};

export default function SeatsMapPage() {
  const navigate = useNavigate();
  const { state } = useLocation();
  const api = useMemo(() => axios.create({ baseURL: API }), []);

  // ✅ Gate/Macro로부터 받은 mno/gno (+ 새로고침 복구)
  const [mno] = useState(() => {
    if (typeof state?.mno === "number") return state.mno;
    const saved = sessionStorage.getItem("gate_mno");
    return saved ? Number(saved) : undefined;
  });
  const [gno] = useState(() => {
    if (typeof state?.gno === "number") return state.gno;
    const saved = sessionStorage.getItem("gate_gno");
    return saved ? Number(saved) : undefined;
  });

  // ✅ 서버에서 내려준 좌석맵 { [sno]: "AVAILABLE" | "HELD" | "HELD_BY_ME" | "SOLD" }
  const [seatMap, setSeatMap] = useState({});
  const [loading, setLoading] = useState(false);
  const [fetchErr, setFetchErr] = useState("");

  // ─────────────────────────────────────────
  // 1) 게이트 세션 검증 (없으면 /gate로)
  // ─────────────────────────────────────────
  useEffect(() => {
    if (!mno || !gno) {
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
  }, [api, mno, gno, navigate]);

  // ─────────────────────────────────────────
  // 2) 좌석 맵 로드 (SeatsController.getMap 사용)
  //    엔드포인트 예시: GET /seats/map?gno=123&mno=20001
  //    응답 예시: { "A-1": "AVAILABLE", "A-2": "SOLD", ... }
  // ─────────────────────────────────────────
  const loadMap = async () => {
    setLoading(true);
    setFetchErr("");
    try {
      const { data } = await api.get("/seats/map", { params: { gno, mno } });
      setSeatMap(data || {});
    } catch (e) {
      setFetchErr("좌석 지도를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (mno && gno) loadMap();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mno, gno]);

  // ─────────────────────────────────────────
  // 3) 좌석 선택/해제 (백엔드 규약에 맞게 sno 사용)
  //    - 선택:  POST /seats/select  { mno, gno, sno }
  //    - 해제:  POST /seats/release { mno, gno, sno }
  //    - SeatLockService.tryLockSeat 반환 코드(-1~)에 맞춰 메시지 처리
  // ─────────────────────────────────────────
  const toggleSeat = async (sno) => {
    const status = seatMap[sno];
    if (status === "SOLD" || status === "HELD") return;

    if (status === "HELD_BY_ME") {
      // 해제
      try {
        const { data } = await api.post("/seats/release", { mno, gno, sno });
        if (data?.ok) {
          // 서버 최신 반영 위해 새로 조회
          await loadMap();
        } else {
          alert(data?.msg || "해제 실패");
        }
      } catch {
        alert("네트워크 오류로 해제 실패");
      }
      return;
    }

    // 선택 (AVAILABLE)
    try {
      const { data } = await api.post("/seats/select", { mno, gno, sno });
      if (data?.ok) {
        await loadMap();
      } else {
        const msg =
          data?.code === -1 ? "세션이 없습니다." :
          data?.code === -2 ? "이미 해당 경기 예매 완료자입니다." :
          data?.code === -3 ? "이미 홀드/매진된 좌석입니다." :
          data?.code === -4 ? "좌석 선택 한도(4개)를 초과했습니다." :
          data?.msg || "선택 실패";
        alert(msg);
      }
    } catch {
      alert("네트워크 오류로 선택 실패");
    }
  };

  // ─────────────────────────────────────────
  // 4) 보기 좋게 정렬/그룹핑
  //    - sno 형태가 "A-1" 처럼 구역-번호면 구역별 그룹핑
  //    - 아니면 전체를 알파벳/숫자 순으로 정렬해서 평면 출력
  // ─────────────────────────────────────────
  const grouped = useMemo(() => {
    const byZone = {};
    Object.entries(seatMap).forEach(([sno, st]) => {
      const [zone] = sno.split("-");
      const key = zone || "기타";
      if (!byZone[key]) byZone[key] = [];
      byZone[key].push({ sno, st });
    });
    // 각 구역 내 정렬
    Object.values(byZone).forEach(list =>
      list.sort((a, b) => a.sno.localeCompare(b.sno, "ko", { numeric: true }))
    );
    return Object.entries(byZone).sort(([a], [b]) => a.localeCompare(b, "ko", { numeric: true }));
  }, [seatMap]);

  return (
    <div className="max-w-6xl mx-auto p-6">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-2xl font-bold">좌석 선택</h2>
          <p className="text-sm text-gray-500">경기 번호: {gno} · 회원: {mno}</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={loadMap}
            className="px-3 py-1 border rounded hover:bg-gray-100"
            disabled={loading}
          >
            {loading ? "불러오는 중…" : "새로고침"}
          </button>
        </div>
      </div>

      {/* 로딩/에러 */}
      {fetchErr && <div className="text-red-600 mb-3 text-sm">{fetchErr}</div>}
      {!fetchErr && Object.keys(seatMap).length === 0 && (
        <div className="text-gray-600 mb-3 text-sm">좌석 데이터가 없습니다.</div>
      )}

      {/* 범례 */}
      <div className="flex flex-wrap gap-4 text-sm mb-5">
        <Legend swatch="bg-green-500" label="선택 가능" />
        <Legend swatch="bg-blue-500" label="내 좌석(임시)" />
        <Legend swatch="bg-gray-300" label="다른사람 홀드" />
        <Legend swatch="bg-gray-400" label="매진" />
      </div>

      {/* 좌석 그리드: 구역별 묶음 */}
      <div className="space-y-6">
        {grouped.map(([zone, seats]) => (
          <div key={zone}>
            <div className="font-semibold mb-2">{zone}</div>
            <div className="flex flex-wrap gap-2">
              {seats.map(({ sno, st }) => (
                <button
                  key={sno}
                  onClick={() => toggleSeat(sno)}
                  disabled={st === "SOLD" || st === "HELD"}
                  title={`${sno} · ${st}`}
                  className={`px-3 py-2 rounded text-xs font-semibold ${COLOR[st] || "bg-gray-200"}`}
                >
                  {sno}
                </button>
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* 하단 안내 */}
      <div className="mt-5 text-xs text-gray-500 leading-5">
        • 임시 보유(HELD_BY_ME)는 서버 TTL(예: 120초) 후 자동 해제됩니다. <br />
        • 예매 완료 시에는 해당 좌석이 <b>SOLD</b>로 전환됩니다.
      </div>
    </div>
  );
}

function Legend({ swatch, label }) {
  return (
    <div className="flex items-center gap-2">
      <span className={`inline-block w-4 h-4 rounded ${swatch}`} />
      <span>{label}</span>
    </div>
  );
}
