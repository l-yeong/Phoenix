import { useEffect, useState } from "react";
import { QRCodeCanvas } from "qrcode.react";

export default function TicketQR({ baseUrl, mno, token }) {
  const [payloads, setPayloads] = useState([]);

  useEffect(() => {
    if (!mno) return;
    fetch(`${baseUrl}/ticket/payloads?mno=${mno}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
      .then(r => r.json())
      .then(setPayloads)
      .catch(console.error);
  }, [baseUrl, mno, token]);

  return (
    <div style={{ display: "grid", gap: 16 }}>
      {payloads.map((p, i) => (
        <div key={i} style={{ display: "flex", gap: 12, alignItems: "center" }}>
          <QRCodeCanvas value={p} size={160} level="M" includeMargin />
          <code style={{ whiteSpace: "pre-wrap" }}>{p}</code>
        </div>
      ))}
      {payloads.length === 0 && <div>표시할 QR 없음</div>}
    </div>
  );
}