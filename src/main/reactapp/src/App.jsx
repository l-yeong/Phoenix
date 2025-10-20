import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Header from "./components/Header";
import Footer from "./components/Footer";
import HomeMain from "./pages/HomeMain";
import AutoReserve from "./pages/AutoReserve";
import SeniorReserve from "./pages/SeniorReserve";
import LoginPage from "./pages/LoginPage";
import SignUpPage from "./pages/SignUpPage";
import SocialSuccess from "./pages/SocialSuccess";
import SocialSignUp from "./pages/SocialSignUp";
import { AuthProvider } from "./api/loginstate.jsx";


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

          {/* ⚙️ 자동 예매 */}
          <Route path="/auto-reserve" element={<AutoReserve />} />

          {/* 👴 시니어 예매 */}
          <Route path="/senior-reserve" element={<SeniorReserve />} />

          {/* 🧩 회원 관련 */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignUpPage />} />
          <Route path="/social/success" element={<SocialSuccess />} />
          <Route path="/social/signup" element={<SocialSignUp />} />
        </Routes>
        <Footer />

      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
