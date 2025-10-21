import axios from "axios";
import { useEffect, useState } from "react";
import TicketQR from "../tickets/TicketQR";
import { useNavigate } from "react-router-dom";

export default function Mypage() {
    const [mode, setMode] = useState("reservation"); // "edit" or "reservation"
    const [reservations, setReservations] = useState([]);
    const [form, setForm] = useState({ mname: "", mphone: "", birthdate: "" });
    const navigate = useNavigate();

    // 예매내역 조회
    const reservePrint = async () => {
        try {
            const response = await axios.get("http://localhost:8080/reserve/print", {
                withCredentials: true,
            });
            setReservations(response.data);
        } catch (e) {
            console.error(e);
        }
    };

    // 회원정보 로드
    const memberInfo = async () => {
        try {
            const response = await axios.get("http://localhost:8080/members/info", {
                withCredentials: true,
            });
            setForm(response.data.data);
        } catch (e) {
            console.error(e);
        }
    };

    // 수정 저장
    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await axios.put("http://localhost:8080/members/update", form, {
                withCredentials: true,
            });
            alert("회원정보 수정 완료!");
        } catch (e) {
            console.error(e);
        }
    };

    useEffect(() => {
        if (mode === "reservation") reservePrint();
        else memberInfo();
    }, [mode]);

    return (
        <div>
            <h2>마이페이지</h2>

            {/* 🔸 탭 메뉴 */}
            <div style={{ marginBottom: "20px" }}>
                <button onClick={() => setMode("reservation")}>예매 내역</button>
                <button onClick={() => setMode("edit")}>회원정보 수정</button>
            </div>

            {/* 🔹 예매내역 */}
            {mode === "reservation" && (
                <>
                    <h3>예매 내역</h3>
                     <div style={{ margin: "20px 0" }}>
                                <TicketQR />
                      </div>
                    {reservations.length === 0 ? (
                        <p>예매 내역이 없습니다.</p>
                    ) : (
                        <table>
                            <thead>
                                <tr>
                                    <th>예매번호</th>
                                    <th>좌석번호</th>
                                    <th>홈팀</th>
                                    <th>어웨이팀</th>
                                    <th>경기날짜</th>
                                    <th>예매현황</th>
                                </tr>
                            </thead>
                            <tbody>
                                {reservations.map((r, idx) => (
                                    <tr key={idx} onClick={() => navigate(`/reservation/${r.reservation.rno}`)} style={{ cursor: "pointer" }}>
                                        <td>{r.reservation.rno}</td>
                                        <td>{r.reservation?.sno}</td>
                                        <td>{r.game.homeTeam}</td>
                                        <td>{r.game.awayTeam}</td>
                                        <td>
                                            {r.game.date} {r.game.time}
                                        </td>
                                        <td>
                                            {r.reservation.status === "reserved"
                                                ? "예매완료"
                                                : r.reservation.status === "cancelled"
                                                    ? "예매취소"
                                                    : r.reservation.status}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </>
            )}

            {/* 🔹 회원정보 수정 */}
            {mode === "edit" && (
                <>
                    <h3>회원정보 수정</h3>
                    <form onSubmit={handleSubmit}>
                        <label>이름</label>
                        <input
                            name="mname"
                            value={form.mname}
                            onChange={(e) =>
                                setForm({ ...form, mname: e.target.value })
                            }
                        />
                        <label>전화번호</label>
                        <input
                            name="mphone"
                            value={form.mphone}
                            onChange={(e) =>
                                setForm({ ...form, mphone: e.target.value })
                            }
                        />
                        <label>생년월일</label>
                        <input
                            name="birthdate"
                            value={form.birthdate}
                            onChange={(e) =>
                                setForm({ ...form, birthdate: e.target.value })
                            }
                        />
                        <button type="submit">수정하기</button>
                    </form>
                </>
            )}
        </div>
    );
}
