import { useState } from "react";

export default function TicketQRList() {

  const [showQR, setShowQR] = useState(false);
  const qrCode = "iVBORw0KGgoAAAANSUhEUgAAAMgAAADIAQAAAACFI5MzAAADPElEQVR4Xu2XTY6jMBCFC3nhHbkAkq/hna8ULgDhAnAl73wNS74A7LywqHkmIcqM1IsUu1FbLWTxtVKl+nlVEP906N8X7/NL/kuyEo2sZlKzLuTKHU8yYrJx6TkO1I2+DLprqYx8gfhuDOWW412rXafVwtQlctex1aXP/IAd3V0kg0Uk0oMUnlv4tPM9qTGgxquNu57Trv+OzpcE+elD9/n3kbmvyXFK42MDO8FMwRxvhGSlrmG6+bSS2qm0Ls1WTragHhrPOObSB55Cml52JGSnOFjUMg24uO6GdGkjJw6Oq9Xxls3s0u7i6OWEQ+l9aUIZLOoRvscxyMluzRIQyBoGcvywsTk9EBDYaS3PrpBFisromTNfIGnJacpxqN0WbwGZP+18T/DbZBFXxCChUQaHjD3tSAiH2r5LSJxLw/Hmy5hPD74nq8aLbiA0MURLPfBf/uWBhDg4i/xA/Mzq1JTTdtoREIYGgBCa2GwIZ4DjTzsSsuU0V8FLSNSq1eYVXyDQqrZWH7UUSZfB4SInGyOEUOKjdqA0ENRXJUoIeyIXD60yXHNOTTZisiGQHjmHjqrVwo5Z9QWSIcaIgZoCDQ6y1/WvLhERNsfkqRp/r7WDnMvJagnNQU4dMejQwaRZTDD5Z+KFq8bfbYLv5zSTEESxzn9LPdf8zBrw9OB7gireqcrVBiXwGEHxnBgiwrG1ZncY1wgDf3awhASerVqC4oxl56jKICfYd7BHDBjaOvYh1dXMs5jsGoWcFqwV4C49LFaeK6QWzuKfpQ1NTQ/99FpCVstLoLvFGls1dcUiEORkt6hBgssNmx1DEhvoWaMiglhClSEw0CokBwblBDoKl2/otqyw4o31eYFA17mmCO2LPQV13ftnfkQE3VaXOxRj7DFsM+aGnKBS6iLG8U4YFzwxvbc0AcHwRwDIYmK/tOqt/hICx11sXey5wy42HZIvJnX3z6ZqVaiq0Dpo89OOhGChwN5UtQp7isYXjznVRURQgChGy/hmmpAifquymJgF7VvnGFL0zraQ1Be4ZQw0RJTe+46E4HsumwWDsa4nuKAF39H5mtSec/g04cUnxqpCdOOnHQn54fySX1LPH8SiZOORZSQ1AAAAAElFTkSuQmCC";

  return (
    <div style={{ textAlign: "center", marginTop: "50px" }}>
      <button
        onClick={() => setShowQR(!showQR)}
        style={{
          padding: "10px 20px",
          backgroundColor: "#007bff",
          color: "white",
          border: "none",
          borderRadius: "8px",
          cursor: "pointer",
        }}
      >
        {showQR ? "QR 숨기기" : "QR 보기"}
      </button>

      {showQR && (
        <div style={{ marginTop: "20px" }}>
          <img
            src={`data:image/png;base64,${qrCode}`}
            alt="QR Code"
            style={{ width: 200, height: 200 }}
          />
        </div>
      )}
    </div>
  );
}