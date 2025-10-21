import { useEffect, useState } from "react";
import axios from "axios";

export default function TicketQR() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState(null);

  // 회원별 티켓 목록 호출 (ticket_code, valid 포함)
  const fetchTickets = async () => {
    try {
      setLoading(true);
      setErr(null);
      const res = await axios.get("http://localhost:8080/tickets/print", {
        withCredentials: true,
      });
      // 기대 응답: [{ tno, ticket_code, valid, issued_at, price, gno, ... }, ...]
      setTickets(res.data || []);
    } catch (e) {
      console.error(e);
      setErr("티켓 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, []);

  if (loading) return <div>불러오는 중...</div>;
  if (err) return <div style={{ color: "crimson" }}>{err}</div>;
  if (!tickets.length) return <div>표시할 QR 티켓이 없습니다.</div>;

  return (
    <div style={{ display: "grid", gap: 16 }}>
      {tickets.map((t, idx) => {
        // valid 값이 boolean/number/string 어느 형태로 와도 처리
        const isValid =
          t?.valid === true || t?.valid === 1 || t?.valid === "1" || t?.valid === "true";

        return (
          <div
            key={t?.tno ?? idx}
            style={{
              border: "1px solid #eee",
              borderRadius: 12,
              padding: 16,
              maxWidth: 260,
            }}
          >
            <div style={{ marginBottom: 8, fontSize: 14, color: "#666" }}>
              {t?.issued_at ? `발급일: ${t.issued_at}` : null}
            </div>

            {isValid ? (
              // ✅ valid=1(true) → QR 이미지 출력
              <img
                src={t?.ticket_code}
                alt="티켓 QR"
                style={{ width: 220, height: 220, objectFit: "contain" }}
              />
            ) : (
              // ✅ valid=0(false) → 안내 문구 출력
              <div style={{ color: "#999", fontWeight: 600, minHeight: 220, display:"flex", alignItems:"center", justifyContent:"center", textAlign:"center", padding:"24px 12px" }}>
                지난 경기 티켓 입니다.
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}