import { useState } from "react";

export default function TicketQRList() {

  const [showQR, setShowQR] = useState(false);
  const qrCode = "iVBORw0KGgoAAAANSUhEUgAAAMgAAADIAQAAAACFI5MzAAADQUlEQVR4Xu2XPY7jMAyFaahwF1/AgK6hTleyL+CfCzhXUqdrCNAF7E6FYe5jEmd2FpgidLcYI0WiL4gUku+RIv7poX8X3s8v+S/JSjTUdq35Hnfye1fvRFZNNsYnItqrsg9124R95AsktIPbe06DN4u394IvXiJ9oVu994U5tn1sL5LB80R5Ijs5nupvJ/iYIAZkF2c2Nodvm/gtOp8S5Kd/HPb9+itzHxM8h8tzSRVj2c7xsaQlK7Xk+fB5JXPQfvN5cRdIzXM090Bj2fuI93k+91EQLu3NoQYJ9TggokhXbfWEM7NZnJ2LXT1Vce+Dnmwl9TFDcJ23R93efBrjFWK3YiaPosYOUF77tc/nBL89Fvx2nlyWSiyZIRc1cXwg4SRm0NWpidSc+ygIh3aMqQlpcHlj0d8hq0pyeMQAFZRQOxXjK/vgXidQEC6QLLwz9bKhgTEs5wkUZHV28tT5vcF7qcTUvfbRkAN+AOejfQxwemjFvvOjICwBMBuKkRnOBz+Yg548vAprEPH+UNs7PxrCAcUIE0Vc247aKprtPLWCYJ8qPL3KckRRU1WsmmzRSgCCndmsrq0QV68nqOueCVUzR3gVbA9n15ONsc/e4LDweASVLFYuEKwReYMYHLU9xPxYTQ7xeL5zGgOaJN7T2c00ZHVS11Uws0woWDOPhqYkGyDMmMREsVUfZZhSk5VSI6aSpxphYLTcvlwg3mI2WessbQ1DmUNze/0fBdli6jxcGT1NGpGMP+f/URB4/ESiOahtqRMRn1OAhmwIZIFKsIbxBDrO06kfDZGt4J2pKnmTkRPjmJ7A42+YEz0ODvEhAPBUPYE47iFjEK4CtIvemIYLBPnBLAyvuhfD4n946QlHwwGWDNtrZdhB8l/50RBorpMRwEDEfZH7xPilxo8J/OAgcy/UBxnVCVpxz1NrCPKDQJJDx8Z9gm4eQrlARHZ58alnc48oTLsFVpNnb5yxEDELYN5pu1ddawjUNnhecJ+TMsRtDOPPBSI3MJlhieCgOPvbldUEUsP/fvQfuTw9Y6AlkmeGJVSMjgFPvUDkPoebqzQfFtvL6Glqgvx0DoHEHd2gEkem5nVqDfnh+SW/RJ4/Z4toCk5+GfIAAAAASUVORK5CYII=";

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