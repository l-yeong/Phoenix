import React, { useState } from "react";
import { QRCodeCanvas } from "qrcode.react";

export default function TestTicketAPI() {
  const baseUrl = "http://localhost:8080/ticket";
  const [rno, setRno] = useState("");
  const [mno, setMno] = useState("");
  const [ticketCode, setTicketCode] = useState("");
  const [payloads, setPayloads] = useState([]);
  const [info, setInfo] = useState(null);
  const [message, setMessage] = useState("");

  // ✅ 1️⃣ 티켓 생성
  async function handleWrite() {
    if (!rno) return alert("rno 입력 필수");
    setMessage("QR 생성 중...");
    try {
      const res = await fetch(`${baseUrl}/write?rno=${rno}`, { method: "POST" });
      const ok = await res.json();
      setMessage(ok ? "✅ QR 생성 완료" : "❌ 생성 실패 (예약상태 확인)");
    } catch (e) {
      console.error(e);
      setMessage("❌ 오류 발생: " + e.message);
    }
  }

  // ✅ 2️⃣ 회원별 QR 리스트 조회
  async function handlePrint() {
    if (!mno) return alert("mno 입력 필수");
    setMessage("QR 목록 조회 중...");
    try {
      const res = await fetch(`${baseUrl}/print?mno=${mno}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setPayloads(Array.isArray(data) ? data : []);
      setMessage(`✅ QR 목록 ${data.length}개 불러옴`);
    } catch (e) {
      console.error(e);
      setMessage("❌ 오류: " + e.message);
    }
  }

  // ✅ 3️⃣ QR 상세정보 조회
  async function handleQrInfo(code) {
    setMessage("QR 상세 조회 중...");
    try {
      const res = await fetch(`${baseUrl}/qrInfo?ticket_code=${encodeURIComponent(code)}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      setInfo(data);
      setMessage("✅ 상세정보 불러오기 완료");
    } catch (e) {
      console.error(e);
      setMessage("❌ 오류: " + e.message);
    }
  }

  return (
    <div style={{ padding: 20, fontFamily: "Arial" }}>
      <h2>🎟 Ticket API 테스트</h2>

      <div style={{ marginBottom: 10 }}>
        <strong>1️⃣ QR 생성 (/ticket/write)</strong>
        <div>
          <input
            placeholder="rno 입력"
            value={rno}
            onChange={(e) => setRno(e.target.value)}
            style={{ marginRight: 8 }}
          />
          <button onClick={handleWrite}>QR 생성</button>
        </div>
      </div>

      <div style={{ marginBottom: 10 }}>
        <strong>2️⃣ 회원별 QR 목록 조회 (/ticket/print)</strong>
        <div>
          <input
            placeholder="mno 입력"
            value={mno}
            onChange={(e) => setMno(e.target.value)}
            style={{ marginRight: 8 }}
          />
          <button onClick={handlePrint}>QR 목록 보기</button>
        </div>
      </div>

      <div style={{ marginTop: 20 }}>
        {payloads.length > 0 && (
          <>
            <h3>QR 목록</h3>
            <div style={{ display: "grid", gap: 12 }}>
              {payloads.map((p, i) => (
                <div
                  key={i}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 10,
                    border: "1px solid #ddd",
                    borderRadius: 6,
                    padding: 8,
                  }}
                >
                  <QRCodeCanvas value={p} size={120} />
                  <div>
                    <div>{p}</div>
                    <button
                      onClick={() => {
                        setTicketCode(p);
                        handleQrInfo(p);
                      }}
                    >
                      상세보기
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </>
        )}
      </div>

      <div style={{ marginTop: 20 }}>
        {info && (
          <>
            <h3>🔍 QR 상세정보</h3>
            <pre
              style={{
                background: "#f5f5f5",
                padding: 10,
                borderRadius: 8,
                whiteSpace: "pre-wrap",
              }}
            >
              {JSON.stringify(info, null, 2)}
            </pre>
          </>
        )}
      </div>

      <div style={{ marginTop: 20, color: "#333" }}>{message}</div>
    </div>
  );
}
