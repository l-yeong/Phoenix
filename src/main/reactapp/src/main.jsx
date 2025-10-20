import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import CssBaseline from "@mui/material/CssBaseline";
import TicketQR from "./features/tickets/TicketQR.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
  <>
    <CssBaseline />
    <App />
  </>
);

// // // QR 이미지테스트 용도
// ReactDOM.createRoot(document.getElementById("root")).render(
//   <React.StrictMode>
//     <TicketQR />
//   </React.StrictMode>
// );