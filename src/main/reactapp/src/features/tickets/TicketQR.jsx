import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams } from "react-router-dom";

function toImgSrc(code) {
  if (!code || typeof code !== "string") return null;

  const head = code.slice(0, 80).toLowerCase();
  if (head.includes("<!doctype html") || head.includes("<html")) return null;

  // 1) data: URL 그대로 허용
  if (code.startsWith("data:image/")) return code;

  // 2) 절대 URL(http/https) 허용
  if (/^https?:\/\//i.test(code)) return code;

  // 3) 서버 정적 경로 처리
  // "/upload/..." 는 8080으로 강제
  if (code.startsWith("/upload/")) return `http://localhost:8080${code}`;

  // 그 외 절대/상대 경로는 그대로 허용
  if (code.startsWith("/")) return code;
  if (code.startsWith("./") || code.startsWith("../")) return code;

  // 4) 긴 base64 문자열 추정 → data URL 생성
  const stripped = code.replace(/\s+/g, "");
  const looksB64 = /^[A-Za-z0-9+/=]+$/.test(stripped) && stripped.length > 100;
  if (looksB64) return `data:image/png;base64,${stripped}`;

  return null;
}

export default function TicketQR({ rno: rnoProp }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  // URL 파라미터 fallback
  const { rno: rnoFromUrl } = useParams();
  const rno = Number(rnoProp ?? rnoFromUrl); // 숫자 변환

  useEffect(() => {
    if (!Number.isFinite(rno)) {
      setErr("유효한 rno가 없습니다.");
      setLoading(false);
      return;
    }

    const token = localStorage.getItem("accessToken");
    const client = axios.create({
      baseURL: "http://localhost:8080", // ✅ 백엔드 직접 호출
      withCredentials: true,
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      validateStatus: () => true,
    });

    (async () => {
      try {
        // ✅ rno 전달
        const res = await client.get("/tickets/print", { params: { rno } });
        const ct = res.headers["content-type"] || "";

        if (res.status !== 200) {
          const snippet =
            typeof res.data === "string"
              ? res.data.slice(0, 200)
              : JSON.stringify(res.data).slice(0, 200);
          throw new Error(`${res.status} ${snippet}`);
        }

        if (!ct.includes("application/json")) {
          const snippet =
            typeof res.data === "string"
              ? res.data.slice(0, 200)
              : JSON.stringify(res.data).slice(0, 200);
          throw new Error(`Expected JSON, got ${ct}: ${snippet}`);
        }

        const arr = Array.isArray(res.data) ? res.data : [];
        // valid가 0/1일 수 있으니 boolean으로 정규화
        setItems(arr.map((t) => ({ ...t, valid: t.valid === true || t.valid === 1 })));
      } catch (e) {
        setErr(e.message || "요청 실패");
      } finally {
        setLoading(false);
      }
    })();
  }, [rno]);

  if (loading) return <div>로딩 중…</div>;
  if (err) return <div style={{ color: "#c00" }}>오류: {err}</div>;
  if (items.length === 0) return <div>티켓이 없습니다.</div>;

  return (
    <div style={{ display: "grid", gap: 16, gridTemplateColumns: "repeat(auto-fill,240px)" }}>
      {items.map((t) => (
        <div key={t.tno} style={{ border: "1px solid #333", borderRadius: 12, padding: 12 }}>
          <div style={{ fontWeight: 700, marginBottom: 8 }}>티켓 #{t.tno}</div>

          {t.valid ? (
            (() => {
              const src = toImgSrc(t.ticket_code);
              return src ? (
                <img
                  src={src}
                  alt="QR"
                  width={220}
                  height={220}
                  style={{ objectFit: "contain", imageRendering: "pixelated" }}
                />
              ) : (
                <div style={{ padding: 12, textAlign: "center", color: "#c00" }}>
                  QR 데이터를 읽을 수 없습니다.
                </div>
              );
            })()
          ) : (
            <div style={{ fontWeight: 600, padding: 12, textAlign: "center" }}>
              사용 불가
            </div>
          )}

          <div style={{ marginTop: 8, fontSize: 12, opacity: 0.7 }}>
            {t.issued_at} · {t.valid ? "사용 가능" : "사용 불가"}
          </div>
        </div>
      ))}
    </div>
  );
}
