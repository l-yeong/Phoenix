import React, { useEffect, useMemo, useState, useCallback } from "react";
import axios from "axios";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "../../styles/zone-seats.css";

const API = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const api = axios.create({ baseURL: API, withCredentials: true });

const HOLD_TTL_SECONDS = 120;

export default function ZoneDemoPage() {
  const { zno } = useParams();          // /zone/:zno
  const znoNum = Number(zno);
  const { state } = useLocation();      // { gno, zoneId, zoneLabel }
  const navigate = useNavigate();

  const gno = state?.gno ?? Number(sessionStorage.getItem("gate_gno") || 0);
  const zoneLabel = state?.zoneLabel ?? `ZNO ${zno}`;

  // ✅ Gate + 파라미터 가드
  useEffect(() => {
    if (!Number.isInteger(znoNum)) {
      alert("잘못된 존 URL입니다."); navigate("/gate", { replace: true }); return;
    }
    if (!Number.isInteger(Number(gno))) {
      navigate("/gate", { replace: true }); return;
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
  }, [gno, znoNum, navigate]);

  // ✅ 좌석 메타
  const [seatsMeta, setSeatsMeta] = useState([]); // [{ sno, seatName }]
  const [metaErr, setMetaErr] = useState("");
  const loadSeatsMeta = useCallback(async () => {
    try {
      setMetaErr("");
      const { data } = await api.get(`/zone/${encodeURIComponent(znoNum)}/seats`);
      setSeatsMeta(Array.isArray(data?.seats) ? data.seats : []);
    } catch {
      setMetaErr("좌석 목록을 불러오지 못했습니다.");
      setSeatsMeta([]);
    }
  }, [znoNum]);
  useEffect(() => { loadSeatsMeta(); }, [loadSeatsMeta]);

  // ✅ 상태
  const [statusBySno, setStatusBySno] = useState({});
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState("");
  const [onlyAvailable, setOnlyAvailable] = useState(false);

  const loadStatus = useCallback(async () => {
    if (!Number.isInteger(Number(gno)) || !Number.isInteger(znoNum)) return;
    if (seatsMeta.length === 0) return;
    setLoading(true);
    setErr("");
    try {
      const seatsPayload = seatsMeta.map((s) => ({ zno: znoNum, sno: s.sno }));
      const { data } = await api.post("/seat/status", { gno, seats: seatsPayload });
      setStatusBySno(data?.statusBySno || {});
    } catch {
      setErr("좌석 상태를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }, [gno, znoNum, seatsMeta]);

  useEffect(() => { loadStatus(); }, [loadStatus]);

  // ✅ 로컬 TTL(2분) — 서버 TTL과 동기화(재방문/새로고침 시 복원)
  const [myTtlBySno, setMyTtlBySno] = useState({});
  useEffect(() => {
    const t = setInterval(() => {
      setMyTtlBySno((prev) => {
        const next = {};
        for (const [k, v] of Object.entries(prev)) {
          const nv = Math.max(0, Number(v) - 1);
          if (nv > 0) next[k] = nv;
        }
        return next;
      });
    }, 1000);
    return () => clearInterval(t);
  }, []);

  const startLocalTtl = useCallback((sno) => {
    setMyTtlBySno((prev) => ({ ...prev, [sno]: HOLD_TTL_SECONDS }));
    sessionStorage.setItem(`holdStartedAt:${gno}:${sno}`, String(Date.now()));
  }, [gno]);

  const clearLocalTtl = useCallback((sno) => {
    setMyTtlBySno((prev) => {
      const next = { ...prev };
      delete next[sno];
      return next;
    });
    sessionStorage.removeItem(`holdStartedAt:${gno}:${sno}`);
  }, [gno]);

  useEffect(() => {
    if (seatsMeta.length === 0) return;
    const now = Date.now();
    const restored = {};
    for (const s of seatsMeta) {
      const key = `holdStartedAt:${gno}:${s.sno}`;
      const started = Number(sessionStorage.getItem(key) || 0);
      if (started > 0 && statusBySno[String(s.sno)] === "HELD_BY_ME") {
        const elapsed = Math.floor((now - started) / 1000);
        const remain = HOLD_TTL_SECONDS - elapsed;
        if (remain > 0) restored[s.sno] = remain;
        else sessionStorage.removeItem(key);
      }
    }
    if (Object.keys(restored).length) {
      setMyTtlBySno((prev) => ({ ...prev, ...restored }));
    }
  }, [gno, seatsMeta, statusBySno]);

  // ✅ 표시용 리스트
  const gridSeats = useMemo(() => {
    const byName = [...seatsMeta].sort((a, b) => {
      const [ar, ac] = [a.seatName[0], Number(a.seatName.slice(1))];
      const [br, bc] = [b.seatName[0], Number(b.seatName.slice(1))];
      if (ar === br) return ac - bc;
      return ar.localeCompare(br);
    });
    return byName
      .map((s) => {
        const st = statusBySno[String(s.sno)] || "AVAILABLE";
        const ttl = st === "HELD_BY_ME" ? (myTtlBySno[String(s.sno)] ?? 0) : 0;
        return { ...s, st, ttl };
      })
      .filter((s) => (onlyAvailable ? s.st === "AVAILABLE" : true));
  }, [seatsMeta, statusBySno, myTtlBySno, onlyAvailable]);

  // ✅ 토글
  const toggleSeat = async ({ sno, st }) => {
    if (st === "SOLD" || st === "HELD") return;

    if (st === "HELD_BY_ME") {
      try {
        const { data } = await api.post("/seat/release", { gno, zno: znoNum, sno });
        if (!data?.ok) return alert("해제 실패");
        clearLocalTtl(sno);
        await loadStatus();
      } catch {
        alert("네트워크 오류로 해제 실패");
      }
      return;
    }

    try {
      const { data } = await api.post("/seat/select", { gno, zno: znoNum, sno });
      if (!data?.ok) {
        const msg =
          data?.code === -1 ? "세션 없음" :
          data?.code === -2 ? "이미 해당 경기 예매됨" :
          data?.code === -3 ? "이미 홀드/매진" :
          data?.code === -4 ? "선택 한도(4개) 초과" :
          data?.code === -5 ? "유효하지 않은 좌석" :
          data?.msg || "선택 실패";
        return alert(msg);
      }
      startLocalTtl(sno);
      await loadStatus();
    } catch {
      alert("네트워크 오류로 선택 실패");
    }
  };

  // ✅ 결제(초기: Redis만 반영)
  const myHeldSnos = useMemo(() => {
    return seatsMeta.map(s => s.sno).filter(sno => statusBySno[String(sno)] === "HELD_BY_ME");
  }, [seatsMeta, statusBySno]);

  // 🔁 기존 confirmSeats 함수를 아래로 교체
  const confirmSeats = async () => {
    if (!myHeldSnos.length) {
      alert("임시 보유한 좌석이 없습니다.");
      return;
    }
    // eslint-disable-next-line no-restricted-globals
    const ok = window.confirm(`${myHeldSnos.length}개 좌석을 결제(확정)하시겠습니까?`);
    if (!ok) return;

    try {
      // 1) 결제(확정)
      const { data } = await api.post("/seat/confirm", { gno, snos: myHeldSnos });

      if (data?.ok) {
        alert("결제(확정) 완료!");

        setMyTtlBySno({});
        myHeldSnos.forEach((sno) => sessionStorage.removeItem(`holdStartedAt:${gno}:${sno}`));

      try {
        await api.post(`/gate/leave`, gno, {
          headers: { "Content-Type": "application/json" },
        });
      } catch (e) {
        alert("[ZoneDemoPage] gate/leave 실패:", e?.message);
      } finally {
        sessionStorage.removeItem("gate_gno");
        navigate("/home", { replace: true });
      }
    }
      await loadStatus();
    } catch {
      alert("네트워크 오류로 결제에 실패했습니다.");
    }
  };

  // ✅ 요약
  const summary = useMemo(() => {
    let avail = 0, mine = 0, sold = 0;
    for (const [, st] of Object.entries(statusBySno)) {
      if (st === "AVAILABLE") avail++;
      else if (st === "HELD_BY_ME") mine++;
      else if (st === "SOLD") sold++;
    }
    return { avail, mine, sold };
  }, [statusBySno]);

  return (
    <div className="zone-page">
      <div className="zone-header">
        <div className="zone-header__left">
          <h2 className="zone-title">좌석 선택 — {zoneLabel}</h2>
          <div className="zone-sub">경기 {gno} · 존 ZNO {zno}</div>
        </div>
        <div className="zone-header__right">
          <button onClick={loadStatus} className="btn btn--ghost" disabled={loading}>
            {loading ? "불러오는 중…" : "새로고침"}
          </button>
          <button onClick={() => navigate(-1)} className="btn btn--ghost">뒤로</button>
          <button onClick={confirmSeats} className="btn btn--primary" disabled={!myHeldSnos.length}>
            결제(확정) · {myHeldSnos.length}석
          </button>
        </div>
      </div>

      <div className="zone-toolbar">
        <Legend swatch="legend__swatch--green" label="선택 가능" />
        <Legend swatch="legend__swatch--blue"  label="내 임시 좌석" />
        <Legend swatch="legend__swatch--hold"  label="다른사람 홀드" />
        <Legend swatch="legend__swatch--sold"  label="매진" />
        <div className="zone-toolbar__spacer" />
        <div className="zone-filter">
          <label className="checkbox">
            <input
              type="checkbox"
              checked={onlyAvailable}
              onChange={(e) => setOnlyAvailable(e.target.checked)}
            />
            <span>선택 가능만</span>
          </label>
          <div className="zone-summary">
            남은 {summary.avail} · 내 임시 {summary.mine} · 매진 {summary.sold}
          </div>
        </div>
      </div>

      {(metaErr || err) && <div className="zone-error">{metaErr || err}</div>}

      {loading && (
        <div className="seat-grid">
          {Array.from({ length: 30 }).map((_, i) => (
            <div key={i} className="seat-skeleton" />
          ))}
        </div>
      )}

      {!loading && (
        <div className="seat-grid">
          {gridSeats.map(({ sno, seatName, st, ttl }) => (
            <button
              key={sno}
              onClick={() => toggleSeat({ sno, st })}
              disabled={st === "SOLD" || st === "HELD"}
              title={`${seatName} · ${st}`}
              className={`seat-chip seat-chip--${st.toLowerCase()}`}
            >
              <span className="seat-label">{seatName}</span>
              {(st === "HELD_BY_ME") && (ttl ?? 0) > 0 && (
                <span className="ttl-badge ttl-badge--mine">{ttl}s</span>
              )}
            </button>
          ))}
        </div>
      )}

      <div className="zone-footer-note">
        • 임시 보유는 2분 후 자동 해제됩니다. (표시 시간은 클라이언트 기준, 새로고침 시 서버와 동기화) <br />
        • 게이트 세션이 만료되면 선택/확정이 차단됩니다.
      </div>
    </div>
  );
}

function Legend({ swatch, label }) {
  return (
    <div className="legend">
      <span className={`legend__swatch ${swatch}`} />
      <span className="legend__label">{label}</span>
    </div>
  );
}
