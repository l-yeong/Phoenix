
// src/pages/Qr.jsx
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import axios from "axios";

export default function Qr() {
  const [sp] = useSearchParams();
  const qr = sp.get("qr");                 // 예: c745b7
  const [data, setData] = useState(null);  // 서버 응답 데이터
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!qr) return; // 쿼리 없으면 호출 안 함
    const controller = new AbortController();

    async function fetchData() {
      try {


        // 1) Vite 프록시를 쓸 경우: baseURL 없이 '/api/...' 로 호출
        // 2) 프록시 없이 직접 호출할 경우: baseURL을 명시
//         const baseURL =
//           import.meta.env.VITE_API_BASE ?? "http://localhost:8080";

        // GET 방식(쿼리스트링) 예시:  GET /api/tickets/qr?qr=c745b7
        const res = await axios.get(`http://localhost:8080/tickets/qr`, {
          params: { qr },
          signal: controller.signal,
          headers: { Accept: "application/json" },
          withCredentials: true, // 쿠키 전송 필요시 true
        });

        setData(res.data);
      } catch (e) {
        if (axios.isCancel(e)) return;
        const msg =
          e.response?.data?.message ||
          e.response?.data?.error ||
          e.message ||
          "관리자만 사용 가능한 페이지 입니다.";
        setError(msg);
      } finally {
        setLoading(false);
      }
    }

    fetchData();
    return () => controller.abort();
  }, [qr]);

  if (!qr) return <div>요청 파라미터가 없습니다. (?qr=값)</div>;
  if (loading) return <div>조회 중...</div>;
  if (error) return <div style={{ color: "crimson" }}>{"관리자만 사용 가능한 페이지 입니다."}</div>;

  return (
    <div style={{ padding: 16 }}>
      <h2>QR 조회 결과</h2>
      {/* 서버가 주는 데이터 구조에 맞춰 렌더링하세요 */}
      <pre style={{ whiteSpace: "pre-wrap" }}>
        {data ? data.message : "데이터가 없습니다."}
      </pre>
    </div>
  );
}
