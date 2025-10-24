import React, { useEffect, useState } from "react";
import { Scanner } from "@yudiel/react-qr-scanner";

export default function QRScanner() {
  const [cams, setCams] = useState([]);
  const [deviceId, setDeviceId] = useState("");
  const [errMsg, setErrMsg] = useState("");

  // 권한 요청 → 장치 나열
  useEffect(() => {
    (async () => {
      try {
        // 1) 먼저 권한을 한 번 요청(팝업)
        await navigator.mediaDevices.getUserMedia({ video: true });
      } catch (e) {
        // 권한 거부하더라도 enumerate를 시도는 해본다
      } finally {
        try {
          const devices = await navigator.mediaDevices.enumerateDevices();
          const videoInputs = devices.filter(d => d.kind === "videoinput");
          setCams(videoInputs);
          if (videoInputs[0]) setDeviceId(videoInputs[0].deviceId);
        } catch (e) {
          setErrMsg(e?.message || String(e));
        }
      }
    })();
  }, []);

  const handleScan = (detected) => {
    const value = Array.isArray(detected) ? detected[0]?.rawValue : detected;
    if (value) {
      console.log("QR 결과:", value);
      alert(`QR 내용: ${value}`);
    }
  };

  const handleError = (err) => {
    console.error("스캔 오류:", err);
    setErrMsg(err?.name ? `${err.name}: ${err.message}` : String(err));
  };

  const constraints = deviceId
    ? { deviceId: { exact: deviceId } } // 사용자가 고른 장치로 정확 지정
    : { facingMode: { ideal: "environment" } }; // 장치가 아직 없으면 완화 제약

  return (
    <div style={{ width: "100%", maxWidth: 460, margin: "0 auto" }}>
      <h2>QR 코드 스캐너</h2>

      {/* 장치 선택 */}
      <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 12 }}>
        <label>카메라:</label>
        <select
          value={deviceId}
          onChange={(e) => setDeviceId(e.target.value)}
          style={{ flex: 1 }}
        >
          {cams.length === 0 && <option value="">감지된 카메라 없음</option>}
          {cams.map((c, i) => (
            <option key={c.deviceId || i} value={c.deviceId}>
              {c.label || `카메라 ${i + 1}`}
            </option>
          ))}
        </select>
      </div>

      {/* 오류 표시 */}
      {errMsg && (
        <div style={{
          background: "#000", color: "#fff", padding: 12, borderRadius: 8, marginBottom: 12
        }}>
          {errMsg}
        </div>
      )}

      <Scanner
        onScan={handleScan}
        onError={handleError}
        constraints={constraints}
        styles={{
          container: {
            borderRadius: 12,
            overflow: "hidden",
            border: "2px solid #1976d2",
            background: "#000",
            minHeight: 260
          },
          video: {
            backgroundColor: "#000"
          }
        }}
      />
    </div>
  );
}
