import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Header from "./components/Header";
import Footer from "./components/Footer";
import HomeMain from "./pages/HomeMain";
import SeniorReserve from "./pages/SeniorReserve";
import LoginPage from "./pages/LoginPage";
import SignUpPage from "./pages/SignUpPage";
import SocialSuccess from "./pages/SocialSuccess";
import SocialSignUp from "./pages/SocialSignUp";
import GatePage from "./features/seats/GatePage";
import MacroPage from "./features/seats/MacroPage";
import SeatsPolygonPage from "./features/seats/SeatsPolygonPage";
import ZoneDemoPage from "./features/seats/ZoneDemoPage";
import FindIdPage from "./pages/FindIdPage";
import FindPwdPage from "./pages/FindPwdPage";
import SeniorSeatAuto from "./pages/SeniorSeatAuto";
import TicketQR from "./features/tickets/TicketQR.jsx";
import TicketQRInfo from "./features/tickets/TicketQRInfo.jsx";
import TicketLog from "./features/tickets/TicketLog";
import QRScanner from "./features/tickets/QRScanner";

import { AuthProvider } from "./api/loginstate.jsx";
import Mypage from "./features/members/Mypage.jsx";
import ReservationFind from "./features/reservations/ReservationFind.jsx"
import ChangeStatus from "./pages/ChangeStatus.jsx";

import Qr from './features/tickets/Qr'
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Header />
        <Routes>
          {/* 메인 선택 페이지 */}
          <Route path="/" element={<HomeMain />} />

          {/* 일반 예매 */}
          <Route path="/home" element={<Home />} />

          {/* 시니어 예매 */}
          <Route path="/senior-reserve" element={<SeniorReserve />} />
          <Route path="/senior/seats" element={<SeniorSeatAuto />} />

          {/* 회원 관련 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/find-id" element={<FindIdPage />} />
          <Route path="/find-pwd" element={<FindPwdPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/social/success" element={<SocialSuccess />} />
          <Route path="/social/signup" element={<SocialSignUp />} />
          <Route path="/changestatus" element={<ChangeStatus />} />
          <Route path="/mypage" element={<Mypage />} />
          <Route path="/reservation/:rno" element={<ReservationFind />} />
          <Route path="/gate" element={<GatePage />} />
          <Route path="/macro" element={<MacroPage />} />
          <Route path="/seats" element={<SeatsPolygonPage />} />
          <Route path="/zone/:zno" element={<ZoneDemoPage />} />

          {/* 티켓 관련 */}
          <Route path="/qr" element={<TicketQRInfo />} />
          <Route path="/tickets/:rno" element={<TicketQR />} />
          <Route path="/tickets/ticketLog" element={<TicketLog />} />
          <Route path="/tickets/QRScanner" element={<QRScanner />} />

          <Route path="/tickets/qr" element={<Qr />} />


        </Routes>
        <Footer />


      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
