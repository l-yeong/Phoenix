import { BrowserRouter, Routes, Route } from "react-router-dom";
import Layout from "./components/Layout";
import Home from "./pages/Home";
import Mypage from "./features/members/mypage";
import ReservationFind from "./features/reservations/ReservationFind";
function App() {
  return (
    <BrowserRouter>      
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Home />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
// <Route path="/mypage" element={<Mypage />} />
// <Route path="/reservationFind/:rno" element={<ReservationFind />} />