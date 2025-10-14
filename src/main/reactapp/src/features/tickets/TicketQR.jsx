import React, { useState } from "react";

export default function TicketQR() {
  const [raw, setRaw] = useState(""); // DB에서 가져온 base64 그대로 붙여넣기
  const [imgSrc, setImgSrc] = useState("");
  const [error, setError] = useState("");

  const handlePreview = () => {
    setError("");
    if (!raw || raw.trim().length === 0) {
      setImgSrc("");
      setError("Base64 문자열을 입력하세요.");
      return;
    }
    // 공백/개행 제거
    const b64 = raw.trim().replace(/\s+/g, "");
    // 만약 'data:image/png;base64,'가 이미 포함되어 있으면 중복으로 붙이지 않기
    const src = b64.startsWith("data:image")
      ? b64
      : `data:image/png;base64,${b64}`;
    setImgSrc(src);
  };

  const handleClear = () => {
    setRaw("");
    setImgSrc("");
    setError("");
  };

  return (
    <div style={{ maxWidth: 720, margin: "24px auto", fontFamily: "sans-serif" }}>
      <h2>Base64 → 이미지 미리보기</h2>

      <textarea
        value={raw}
        onChange={(e) => setRaw(e.target.value)}
        placeholder="여기에 DB의 base64 문자열을 그대로 붙여넣으세요 (data:image... 접두어가 있어도/없어도 됩니다)"
        rows={8}
        style={{ width: "100%", fontFamily: "monospace" }}
      />

      <div style={{ marginTop: 8, display: "flex", gap: 8 }}>
        <button onClick={handlePreview}>미리보기</button>
        <button onClick={handleClear}>초기화</button>
        {imgSrc && (
          <a href={imgSrc} download={"qr.png"}>
            <button>다운로드</button>
          </a>
        )}
      </div>

      {error && (
        <div style={{ color: "crimson", marginTop: 8 }}>{error}</div>
      )}

      <div style={{ marginTop: 16 }}>
        {imgSrc ? (
          <img
            src={imgSrc}
            alt="preview"
            width={220}
            height={220}
            style={{ objectFit: "contain", border: "1px solid #ddd" }}
            onError={() => setError("이미지 렌더 실패: Base64가 손상되었을 수 있습니다.")}
          />
        ) : (
          <div style={{ color: "#666" }}>이미지가 여기에 표시됩니다.</div>
        )}
      </div>
    </div>
  );
}