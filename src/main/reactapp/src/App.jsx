import { BrowserRouter, Routes, Route } from "react-router-dom";
import Home from "./pages/Home";
import Header from "./components/Header";
import LoginPage from "./pages/LoginPage";
import SignUpPage from "./pages/SignUpPage";
import SocialSuccess from "./pages/SocialSuccess";
import SocialSignUp from "./pages/SocialSignUp";


function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignUpPage />} />
        <Route path="/social/success" element={<SocialSuccess />} />
        <Route path="/social/signup" element={<SocialSignUp />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
