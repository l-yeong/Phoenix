// ✅ React 컴포넌트 이름은 파일명과 동일하게 유지
export default function TicketQR({ ticket }) {
  const API_BASE = import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

  // ✅ 이건 내부에서 URL 보정용으로만 쓰이는 함수
  const toImageSrc = (raw) => {
    if (!raw) return "";
    let p = String(raw).trim();
    p = p.replaceAll("\\", "/");
    if (p.startsWith("http://") || p.startsWith("https://")) return p;
    const ensureLeadingSlash = p.startsWith("/") ? p : `/${p}`;
    const parts = ensureLeadingSlash.split("/");
    const prefix = parts.slice(0, 3).join("/");
    const rest = parts.slice(3).map(encodeURIComponent).join("/");
    const normalized = rest ? `${prefix}/${rest}` : ensureLeadingSlash;
    const fixed = normalized.startsWith("/uploads/")
      ? normalized.replace("/uploads/", "/upload/")
      : normalized;
    return `${API_BASE}${fixed}`;
  };

  return (
    <div>
      <img
        src={toImageSrc(ticket.ticket_code)}
        alt="티켓 QR"
        style={{ width: 200, height: 200, objectFit: "contain" }}
      />
    </div>
  );
}
