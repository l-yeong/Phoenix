import React from "react";
import { Scanner } from "@yudiel/react-qr-scanner";

export default function QRScanner() {
  const handleScan = (detected) => {
    // detected: IDetectedBarcode[] (여러 개가 올 수 있음)
    const value = Array.isArray(detected) ? detected[0]?.rawValue : detected;
    if (value) {
      console.log("QR 결과:", value);
      alert(`QR 내용: ${value}`);
    }
  };

  const handleError = (err) => {
    console.error("스캔 오류:", err?.message || err);
  };

  return (
    <div style={{ width: "100%", maxWidth: 400, margin: "0 auto" }}>
      <h2>QR 코드 스캐너</h2>
      <Scanner
        onScan={handleScan}
        onError={handleError}
        constraints={{ facingMode: "environment" }}
        styles={{
          container: { borderRadius: 12, overflow: "hidden", border: "2px solid #1976d2" },
        }}
      />
    </div>
  );
}
