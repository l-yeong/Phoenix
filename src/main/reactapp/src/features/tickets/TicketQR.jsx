// import { useEffect, useState } from "react";
// import { QRCodeCanvas } from "qrcode.react";
//
// export default function TicketQR({ baseUrl, mno, token }) {
//   const [payloads, setPayloads] = useState([]);
//
//   useEffect(() => {
//     if (!mno) return;
//     fetch(`${baseUrl}/ticket/payloads?mno=${mno}`, {
//       headers: token ? { Authorization: `Bearer ${token}` } : {},
//     })
//       .then(r => r.json())
//       .then(setPayloads)
//       .catch(console.error);
//   }, [baseUrl, mno, token]);
//
//   return (
//     <div style={{ display: "grid", gap: 16 }}>
//       {payloads.map((p, i) => (
//         <div key={i} style={{ display: "flex", gap: 12, alignItems: "center" }}>
//           <QRCodeCanvas value={p} size={160} level="M" includeMargin />
//           <code style={{ whiteSpace: "pre-wrap" }}>{p}</code>
//         </div>
//       ))}
//       {payloads.length === 0 && <div>표시할 QR 없음</div>}
//     </div>
//   );
// }
// src/components/TestTicketPayloads.jsx
  import React, { useEffect, useState } from "react";
  import { QRCodeCanvas } from "qrcode.react";

  export default function TestTicketPayloads() {
    const [payloads, setPayloads] = useState([]);
    const [selected, setSelected] = useState(null); // 선택된 QR코드(상세 조회)
    const [scanResult, setScanResult] = useState(null); // /scan 결과
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const mno = 20011;
    const token =
      "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0IiwiaWF0IjoxNzYwNTE4MDQ3LCJleHAiOjE3NjA1MjE2NDcsIm1ubyI6MjAwMTF9.MFO6iM4rJzboA94_D3tS4Q0wepdRAOYnUzTFmCcwLsBWEFVuQ-SLqhYG-UZ2xG29";
    const baseUrl = "http://localhost:8080";

    // [1] payload 리스트 조회
    useEffect(() => {
      async function fetchPayloads() {
        setLoading(true);
        setError(null);
        try {
          const res = await fetch(`${baseUrl}/ticket/payloads?mno=${mno}`, {
            headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
          });
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          const data = await res.json();
          setPayloads(Array.isArray(data) ? data : []);
        } catch (e) {
          console.error(e);
          setError(String(e));
        } finally {
          setLoading(false);
        }
      }
      fetchPayloads();
    }, [baseUrl, mno, token]);

    // [2] 특정 QR 스캔 정보 가져오기
    async function handleScan(code) {
      setSelected(code);
      setScanResult(null);
      try {
        const res = await fetch(`${baseUrl}/ticket/scan?code=${encodeURIComponent(code)}`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "application/json" },
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        setScanResult(data);
      } catch (e) {
        console.error(e);
        setScanResult({ 에러: e.message });
      }
    }

    return (
      <div style={{ padding: 16 }}>
        <h3>테스트: Ticket Payloads (mno={mno})</h3>

        {loading && <div>로딩 중...</div>}
        {error && <div style={{ color: "red" }}>에러: {error}</div>}

        <div style={{ display: "grid", gap: 18, marginTop: 12 }}>
          {payloads.map((p, i) => (
            <div key={i} style={{ display: "flex", alignItems: "center", gap: 10 }}>
              <QRCodeCanvas value={p} size={120} />
              <div>
                <div style={{ fontSize: 13, marginBottom: 4 }}>{p}</div>
                <button onClick={() => handleScan(p)}>QR 스캔 결과 보기</button>
              </div>
            </div>
          ))}
        </div>

        {selected && (
          <div style={{ marginTop: 20 }}>
            <h4>QR 스캔 결과 ({selected})</h4>
            <pre style={{ background: "#f5f5f5", padding: 10 }}>
              {scanResult ? JSON.stringify(scanResult, null, 2) : "조회 중..."}
            </pre>
          </div>
        )}
      </div>
    );
  }

