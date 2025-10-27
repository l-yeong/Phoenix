// src/features/admin/TicketLog.jsx
import React, { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../api/axiosInstance";

export default function TicketLog() {
  const navigate = useNavigate();
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [q, setQ] = useState("");
  const [status, setStatus] = useState("ALL");
  const [valid, setValid] = useState("ALL");
  const [error, setError] = useState("");

  // ✅ 진입 가드: admin 계정만 접근 허용
  useEffect(() => {
    let ignore = false;
    (async () => {
      try {
        // 로그인 사용자 조회 (세션/쿠키 기반이면 credentials 포함)
        const me = await api.get("/members/info", { withCredentials: true });
        const mid = me?.data?.mid;
        if (!mid || mid.toLowerCase() !== "admin") {
          alert("관리자 페이지입니다.");
          navigate("/", { replace: true });
          return;
        }
        if (!ignore) fetchData(); // 통과 시 목록 불러오기
      } catch (e) {
        // 로그인 안됨 또는 권한 없음
        alert("관리자 페이지입니다.");
        navigate("/", { replace: true });
      }
    })();
    return () => { ignore = true; };
  }, [navigate]);

  const fetchData = async () => {
    setLoading(true);
    setError("");
    try {
      const res = await api.get("/tickets/ticketLog", { withCredentials: true });
      if (res.status === 200 && Array.isArray(res.data)) {
        setRows(res.data);
      } else {
        console.log("응답 데이터:", res.data);
        setError(`목록 조회 실패: ${res.status}`);
      }
    } catch (e) {
      const code = e?.response?.status;
      // ✅ 서버가 401/403을 주면 즉시 차단
      if (code === 401 || code === 403) {
        alert("관리자 페이지입니다.");
        navigate("/", { replace: true });
        return;
      }
      setError(e?.message || "네트워크 오류");
    } finally {
      setLoading(false);
    }
  };

  const filtered = useMemo(() => {
    return rows
      .filter((r) => {
        const target = `${r.mname ?? ""} ${r.mphone ?? ""}`.toLowerCase();
        const okQ = target.includes(q.trim().toLowerCase());
        const okStatus = status === "ALL" ? true : r.reservation_status === status;
        let okValid = true; // valid: 1=미사용, 0=사용됨
        if (valid === "USED") okValid = r.valid === 0 || r.valid === false;
        if (valid === "UNUSED") okValid = r.valid === 1 || r.valid === true;
        return okQ && okStatus && okValid;
      })
      .sort((a, b) => {
        const av = a.valid ? 1 : 0;
        const bv = b.valid ? 1 : 0;
        if (av !== bv) return av - bv; // 미사용 먼저
        return String(b.seat_label || "").localeCompare(String(a.seat_label || ""));
      });
  }, [rows, q, status, valid]);

  const fmtPrice = (n) => (n == null ? "-" : `₩${Number(n).toLocaleString()}`);
  const badgeStatus = (s) => {
    const base = { padding: "2px 8px", borderRadius: 8, fontSize: 12, display: "inline-block" };
    if (s === "reserved") return <span style={{ ...base, background: "#e3f2fd" }}>reserved</span>;
    if (s === "cancelled") return <span style={{ ...base, background: "#ffe0e0" }}>cancelled</span>;
    return <span style={{ ...base, background: "#eee" }}>{s}</span>;
  };
  const badgeValid = (v) => {
    const ok = v === 1 || v === true;
    const base = { padding: "2px 8px", borderRadius: 8, fontSize: 12, display: "inline-block" };
    return ok
      ? <span style={{ ...base, background: "#fff3cd" }}>❌ 미사용</span>
      : <span style={{ ...base, background: "#d1e7dd" }}>✅ 사용 완료</span>;
  };

  const exportCSV = () => {
    const cols = ["mname","mphone","zname","seat_no","seat_label","seat_price","reservation_status","valid"];
    const header = cols.join(",");
    const body = filtered.map(r => [
      r.mname ?? "", r.mphone ?? "", r.zname ?? "", r.seat_no ?? "",
      r.seat_label ?? "", r.seat_price ?? "", r.reservation_status ?? "",
      (r.valid ? "미사용" : "사용완료")
    ].map(v => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
    const blob = new Blob([header + "\n" + body], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = "ticket_log.csv"; a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div style={{ maxWidth: 1100, margin: "24px auto", padding: 16 }}>
      <div style={{ display: "flex", alignItems: "center", marginBottom: 12, gap: 8 }}>
        <h2 style={{ margin: 0, flex: 1 }}>관리자 QR 사용 기록</h2>
        <button
          onClick={() => navigate("/tickets/QRScanner")}
          style={{
            padding: "8px 12px",
            borderRadius: 8,
            border: "1px solid #1976d2",
            background: "#1976d2",
            color: "#fff",
            fontWeight: 600,
          }}
        >
          QR 스캐너
        </button>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 160px 160px auto", gap: 8, marginBottom: 12 }}>
        <input value={q} onChange={(e) => setQ(e.target.value)} placeholder="이름/연락처 검색"
               style={{ padding: 8, border: "1px solid #ccc", borderRadius: 8 }} />
        <select value={status} onChange={(e) => setStatus(e.target.value)} style={{ padding: 8, borderRadius: 8 }}>
          <option value="ALL">전체 상태</option>
          <option value="reserved">reserved</option>
          <option value="cancelled">cancelled</option>
        </select>
        <select value={valid} onChange={(e) => setValid(e.target.value)} style={{ padding: 8, borderRadius: 8 }}>
          <option value="ALL">전체 사용여부</option>
          <option value="UNUSED">미사용만</option>
          <option value="USED">사용완료만</option>
        </select>
        <div style={{ textAlign: "right" }}>
          <button onClick={fetchData} style={{ padding: "8px 12px", marginRight: 8, borderRadius: 8 }}>새로고침</button>
          <button onClick={exportCSV} style={{ padding: "8px 12px", borderRadius: 8 }}>CSV</button>
        </div>
      </div>

      {error && <div style={{ marginBottom: 8, color: "#c62828" }}>⚠ {error}</div>}
      {loading && <div style={{ marginBottom: 8, opacity: 0.7 }}>불러오는 중…</div>}

      <div style={{ overflowX: "auto" }}>
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr style={{ background: "#f5f5f5" }}>
              <th style={th}>회원명</th>
              <th style={th}>연락처</th>
              <th style={th}>구역</th>
              <th style={th}>좌석</th>
              <th style={th}>구역/좌석</th>
              <th style={th}>가격</th>
              <th style={th}>예약상태</th>
              <th style={th}>사용여부</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((r, i) => (
              <tr key={i} style={{ borderTop: "1px solid #eee" }}>
                <td style={td}>{r.mname ?? "-"}</td>
                <td style={td}>{r.mphone ?? "-"}</td>
                <td style={td}>{r.zname ?? "-"}</td>
                <td style={td}>{r.seat_no ?? "-"}</td>
                <td style={td}>{r.seat_label ?? "-"}</td>
                <td style={td}>{fmtPrice(r.seat_price)}</td>
                <td style={td}>{badgeStatus(r.reservation_status)}</td>
                <td style={td}>{badgeValid(r.valid)}</td>
              </tr>
            ))}
            {!loading && filtered.length === 0 && (
              <tr>
                <td colSpan={8} style={{ padding: 24, textAlign: "center", color: "#777" }}>
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

const th = { padding: "10px 8px", textAlign: "left", fontWeight: 600, fontSize: 14, borderBottom: "1px solid #e0e0e0" };
const td = { padding: "10px 8px", fontSize: 14 };
