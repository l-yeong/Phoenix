// src/QrTicketInfo.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";

export default function TicketQRInfo() {
  const [data, setData] = useState(null);
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(true);

  const params = new URLSearchParams(window.location.search);
  const uuid = params.get("qr");

  useEffect(() => {
    (async () => {
      try {
        if (!uuid) throw new Error("QR 파라미터(qr)가 없습니다.");

        const token = localStorage.getItem("accessToken");
        const client = axios.create({
          withCredentials: true,
          headers: token ? { Authorization: `Bearer ${token}` } : {},
          validateStatus: () => true,
        });

        const res = await client.get("/tickets/qrInfo", { params: { qr: uuid } });
        if (res.status !== 200) {
          const snippet =
            typeof res.data === "string" ? res.data.slice(0, 200) : JSON.stringify(res.data).slice(0, 200);
          throw new Error(`${res.status} ${snippet}`);
        }

        setData(res.data);
      } catch (e) {
        setErr(e.message || "요청 실패");
      } finally {
        setLoading(false);
      }
    })();
  }, [uuid]);

  if (loading) return <div style={{ padding: 24 }}>로딩 중...</div>;
  if (err) return <div style={{ padding: 24, color: "#c00" }}>오류: {err}</div>;
  if (!data) return <div style={{ padding: 24 }}>데이터가 없습니다.</div>;

  const { mname, zname, seat_no, valid, seat_price } = data;

  // valid가 DB/드라이버 설정에 따라 true/false 또는 1/0으로 들어올 수 있으므로 안전 처리
  const isValid =
    valid === true || valid === 1 || valid === "1" || String(valid).toLowerCase() === "true";

  return (
    <div style={{
      maxWidth: 520,
      margin: "40px auto",
      padding: 20,
      border: "1px solid #e5e7eb",
      borderRadius: 12,
      boxShadow: "0 2px 10px rgba(0,0,0,0.05)"
    }}>
      <h2 style={{ marginTop: 0, marginBottom: 16 }}>예매 정보</h2>
      <div style={{ display: "grid", gridTemplateColumns: "120px 1fr", rowGap: 10 }}>
        <div style={{ fontWeight: 600 }}>이름</div><div>{mname ?? "-"}</div>
        <div style={{ fontWeight: 600 }}>구역</div><div>{zname ?? "-"}</div>
        <div style={{ fontWeight: 600 }}>좌석</div><div>{seat_no ?? "-"}</div>
        <div style={{ fontWeight: 600 }}>사용여부</div><div>{isValid ? "유효" : "만료/사용불가"}</div>
        <div style={{ fontWeight: 600 }}>가격</div><div>{Number(seat_price ?? 0).toLocaleString()} 원</div>
      </div>
    </div>
  );
}
