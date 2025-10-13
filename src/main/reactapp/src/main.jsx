import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import CssBaseline from "@mui/material/CssBaseline";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <CssBaseline />
    <App />
  </React.StrictMode>
);

// // QR 이미지테스트 용도
// const create = createRoot(root);
//
// import TicketQR from './features/tickets/TicketQR';
// create.render(
//       <TicketQR />
// );