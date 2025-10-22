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

import { AuthProvider } from "./api/loginstate.jsx";
import Mypage from "./features/members/Mypage.jsx";
import ReservationFind from "./features/reservations/ReservationFind.jsx"
import SeatChange from "./features/reservationExchanges/SeatChange.jsx";

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>

        <Header />
        <Routes>
          {/* 🔴 메인 선택 페이지 */}
          <Route path="/" element={<HomeMain />} />

          {/* ⚾ 일반 예매 */}
          <Route path="/home" element={<Home />} />

          {/* 👴 시니어 예매 */}
          <Route path="/senior-reserve" element={<SeniorReserve />} />

          {/* 🧩 회원 관련 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/social/success" element={<SocialSuccess />} />
          <Route path="/social/signup" element={<SocialSignUp />} />
          <Route path="/mypage" element={<Mypage />} />
          <Route path="/reservation/:rno" element={<ReservationFind />} />
          <Route path="/gate" element={<GatePage />} />
          <Route path="/macro" element={<MacroPage />} />
          <Route path="/seats" element={<SeatsPolygonPage />} />
          <Route path="/zone/:zoneId" element={<ZoneDemoPage />} />
          <Route path="/seatchange" element={<SeatChange /> } />
        </Routes>
        <Footer />

      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
