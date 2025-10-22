import axios from "axios";
import { useEffect, useState } from "react";
import TicketQR from "../tickets/TicketQR";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../api/loginstate";

export default function Mypage() {
    const [mode, setMode] = useState("reservation"); // reservation | edit | password | delete
    const [reservations, setReservations] = useState([]);
    const [form, setForm] = useState({
        mname: "",
        mphone: "",
        email: "",
        birthdate: "",
        pno: "",
        exchange: false,
    })

    const [provider, setProvider] = useState(null); // 소셜 회원 여부 저장

    const { logout } = useAuth();
    ;

    const [passwordForm, setPasswordForm] = useState({
        current_password: "",
        new_password: "",
    });

    const [deleteForm, setDeleteForm] = useState({ password_hash: "" });
    const navigate = useNavigate();

    /* ===============================
       예매내역 조회
    =============================== */
    const reservePrint = async () => {
        try {
            const response = await axios.get("http://localhost:8080/reserve/print", {
                withCredentials: true,
            });
            setReservations(response.data);
        } catch (e) {
            console.error("예매내역 조회 실패:", e);
        }
    };

    /* ===============================
       회원정보 로드
    =============================== */
    const memberInfo = async () => {
        try {
            const response = await axios.get("http://localhost:8080/members/info", {
                withCredentials: true,
            });
            const data = response.data.data;
            setForm(data);
            setProvider(data.provider || null); // 소셜 회원 여부 설정
        } catch (e) {
            console.error("회원정보 로드 실패:", e);
        }
    };

    /* ===============================
       회원정보 수정
    =============================== */
    const handleInfoUpdate = async (e) => {
        e.preventDefault();
        try {

            const payload = { ...form };
            // 소셜 회원인 경우 이메일, 전화번호 수정 불가
            if (provider) {
                delete payload.password_hash;
            }

            const res = await axios.put(
                "http://localhost:8080/members/infoupdate",
                payload,
                { withCredentials: true }
            );
            if (res.data.success) alert("회원정보가 성공적으로 수정되었습니다!");
            else alert(res.data.message);
        } catch (e) {
            console.error("회원정보 수정 실패:", e);
            alert("서버 오류가 발생했습니다.");
        }
    };

    /* ===============================
       비밀번호 변경
    =============================== */
    const handlePasswordUpdate = async (e) => {
        e.preventDefault();
        try {
            const res = await axios.put(
                "http://localhost:8080/members/pwdupdate",
                passwordForm,
                { withCredentials: true }
            );
            if (res.data.success) {
                alert("비밀번호가 성공적으로 변경되었습니다!");
                setPasswordForm({ current_password: "", new_password: "" });
            } else {
                alert(res.data.message);
            }
        } catch (e) {
            console.error("비밀번호 변경 실패:", e);
            alert("서버 오류가 발생했습니다.");
        }
    };

    /* ===============================
       회원탈퇴
    =============================== */
    const handleDelete = async (e) => {
        e.preventDefault();
        if (!window.confirm("정말 탈퇴하시겠습니까? 복구할 수 없습니다.")) return;

        try {

            // 소셜 회원이면 비밀번호 입력 없이 빈 객체 전송
            const payload = provider ? {} : deleteForm;

            const res = await axios.post(
                "http://localhost:8080/members/delete",
                payload,
                { withCredentials: true }
            );
            if (res.data.success) {
                alert("회원 탈퇴가 완료되었습니다.");
                logout();
            } else {
                alert(res.data.message);
            }
        } catch (error) {
            const { status, data } = error.response;
            if (status === 401) {
                alert("아이디 또는 비밀번호를 확인해주세요.");
            } else if (status === 403) {
                alert(data.message); // "탈퇴한 계정입니다." / "휴면 상태의 계정입니다."
            } else {
                alert("서버 오류가 발생했습니다.");
            }
        }
    };

    /* ===============================
       모드 전환 시 데이터 로드
    =============================== */
    useEffect(() => {
        if (mode === "reservation") reservePrint();
        else memberInfo();
    }, [mode]);

    /* ===============================
       렌더링
    =============================== */
    return (
        <div style={{ padding: "30px" }}>
            <h2>마이페이지</h2>

            {/* 탭 메뉴 */}
            <div style={{ marginBottom: "20px" }}>
                <button onClick={() => setMode("reservation")}>예매 내역</button>
                <button onClick={() => setMode("edit")}>회원정보 수정</button>
                {/* 소셜회원이면 비밀번호 변경 탭 숨김 */}
                {!provider && (
                    <button onClick={() => setMode("password")}>비밀번호 변경</button>
                )}
                <button onClick={() => setMode("delete")}>회원 탈퇴</button>
            </div>

            {/* 예매내역 */}
            {mode === "reservation" && (
                <>
                    <h3>예매 내역</h3>
                    {reservations.length === 0 ? (
                        <p>예매 내역이 없습니다.</p>
                    ) : (
                        <table border="1" cellPadding="8" style={{ borderCollapse: "collapse" }}>
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
                                    <tr
                                        key={idx}
                                        onClick={() => navigate(`/reservation/${r.reservation.rno}`)}
                                        style={{ cursor: "pointer" }}
                                    >
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

            {/* 회원정보 수정 */}
            {mode === "edit" && (
                <>
                    <h3>회원정보 수정</h3>
                    <form onSubmit={handleInfoUpdate}>
                        <label>이름</label>
                        <input
                            name="mname"
                            value={form.mname || ""}
                            onChange={(e) => setForm({ ...form, mname: e.target.value })}
                        />
                        <label>전화번호</label>
                        <input
                            name="mphone"
                            value={form.mphone || ""}
                            onChange={(e) => setForm({ ...form, mphone: e.target.value })}
                        />

                        <label>이메일</label>
                        <input
                            name="email"
                            value={form.email || ""}
                            onChange={(e) => setForm({ ...form, email: e.target.value })}
                        />

                        <label>선호 선수 번호(pno)</label>
                        <input
                            name="pno"
                            value={form.pno || ""}
                            onChange={(e) => setForm({ ...form, pno: e.target.value })}
                        />

                        <label>교환 여부</label>
                        <input
                            type="checkbox"
                            checked={form.exchange}
                            onChange={(e) => setForm({ ...form, exchange: e.target.checked })}
                        />

                        <button type="submit">수정하기</button>
                    </form>
                </>
            )}
            {/* 일반회원만 비밀번호 변경 가능 */}
            {!provider && mode === "password" && (
                <>
                    <h3>비밀번호 변경</h3>
                    <form onSubmit={handlePasswordUpdate}>
                        <label>현재 비밀번호</label>
                        <input
                            type="password"
                            name="current_password"
                            value={passwordForm.current_password}
                            onChange={(e) =>
                                setPasswordForm({
                                    ...passwordForm,
                                    current_password: e.target.value,
                                })
                            }
                        />

                        <label>새 비밀번호</label>
                        <input
                            type="password"
                            name="new_password"
                            value={passwordForm.new_password}
                            onChange={(e) =>
                                setPasswordForm({
                                    ...passwordForm,
                                    new_password: e.target.value,
                                })
                            }
                        />

                        <button type="submit">변경하기</button>
                    </form>
                </>
            )}

            {/* 회원 탈퇴 */}
            {mode === "delete" && (
                <>
                    <h3>회원 탈퇴</h3>
                    <form onSubmit={handleDelete}>
                        {/* 🔹 소셜회원이면 비밀번호 입력칸 제거 */}
                        {!provider && (
                            <>
                                <label>비밀번호 확인</label>
                                <input
                                    type="password"
                                    name="password_hash"
                                    value={deleteForm.password_hash}
                                    onChange={(e) =>
                                        setDeleteForm({
                                            ...deleteForm,
                                            password_hash: e.target.value,
                                        })
                                    }
                                />
                            </>
                        )}
                        <button type="submit" style={{ backgroundColor: "red", color: "white" }}>
                            탈퇴하기
                        </button>
                    </form>
                </>
            )}
        </div>
    );
}
