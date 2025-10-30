import axios from "axios";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../api/loginstate";
import {
    Button, Box, Card, CardContent, CardActions, Checkbox, Divider,
    FormControl, FormLabel, Input, Sheet, Stack, Table as JoyTable,
    Typography, Chip, Alert, Select, Option
} from "@mui/joy";

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
    });

    // 유효성 오류 상태
    const [errors, setErrors] = useState({
        mname: "",
        mphone: "",
        email: "",
        current_password: "",
        new_password: "",
    });

    const [provider, setProvider] = useState(null); // 소셜 회원 여부 저장

    const { logout } = useAuth();
    ;

    const [passwordForm, setPasswordForm] = useState({
        current_password: "",
        new_password: "",
    });

    const [deleteForm, setDeleteForm] = useState({ password_hash: "" });
    const navigate = useNavigate();

    const [players, setPlayers] = useState([]);

    useEffect(() => {
        const fetchPlayers = async () => {
            try {
                const res = await axios.get("http://192.168.40.190:8080/game/players");
                if (res.data.success) {
                    setPlayers(res.data.data);
                }
            } catch (e) {
                console.error("선수 목록 로드 실패:", e);
            }
        };
        fetchPlayers();
    }, []);

    /* ===============================
       예매내역 조회
    =============================== */
    const reservePrint = async () => {
        try {
            const response = await axios.get("http://192.168.40.190:8080/reserve/print", {
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
            const response = await axios.get("http://192.168.40.190:8080/members/info", {
                withCredentials: true,
            });
            const data = response.data.data;
            setForm(data);
            setProvider(!!data.provider); // 소셜 회원 여부 설정
        } catch (e) {
            console.error("회원정보 로드 실패:", e);
        }
    };

    // ===============================
    // 입력값 유효성 검사 함수
    // ===============================
    const validateField = (name, value) => {
        let error = "";

        switch (name) {
            case "mname":
                if (!value.trim()) {
                    error = "이름을 입력하세요.";
                } else if (value.trim().length < 2) {
                    error = "이름은 2자 이상이어야 합니다.";
                } else if (!/^[가-힣a-zA-Z]+$/.test(value)) {
                    error = "이름에는 숫자나 특수문자를 포함할 수 없습니다.";
                }
                break;

            case "mphone":
                if (!/^010-\d{4}-\d{4}$/.test(value))
                    error = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)";
                break;

            case "email":
                if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.(com|net|org|co\.kr|kr|ac\.kr)$/.test(value)) {
                    error = "이메일 형식이 올바르지 않습니다. (예: example@gmail.com)";
                }
                break;

            case "current_password":
                if (!value) error = "현재 비밀번호를 입력하세요.";
                break;

            case "new_password":
                if (
                    !/^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{8,}$/.test(
                        value
                    )
                )
                    error = "비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.";
                break;

            default:
                break;
        }

        setErrors((prev) => ({ ...prev, [name]: error }));
        return error === "";
    };

    /* ===============================
       회원정보 수정
    =============================== */
    const handleInfoUpdate = async (e) => {
        e.preventDefault();

        // 유효성 검사 (이름 , 전화번호 , 이메일)
        const valid =
            validateField("mname", form.mname) &
            validateField("mphone", form.mphone) &
            validateField("email", form.email);
        if (!valid) {
            alert("입력한 정보를 다시 확인해주세요.");
            return;
        }

        try {

            const payload = { ...form, pno: Number(form.pno) }; // 숫자로 변환해서 주기
            // 소셜 회원인 경우 이메일, 전화번호 수정 불가
            if (provider) {
                delete payload.password_hash;
            }

            const res = await axios.put(
                "http://192.168.40.190:8080/members/infoupdate",
                payload,
                { withCredentials: true }
            );
            if (res.data.success) {
                alert("회원정보가 성공적으로 수정되었습니다!");

                // ✅ 서버가 수정된 회원정보를 반환한다면
                if (res.data.data) {
                    setForm(res.data.data); // form 전체를 서버 응답으로 갱신
                } else {
                    // ✅ 서버가 반환 안 하는 경우, 직접 form 그대로 유지
                    setForm(payload);
                }

                setMode("reservation"); // 수정 후 다시 기본 모드로 전환할 수도 있음
            } else { alert(res.data.message); }
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

        // 유효성 검사 (현재 비밀번호 , 새 비밀번호)
        const valid =
            validateField("current_password", passwordForm.current_password) &
            validateField("new_password", passwordForm.new_password);

        if (!valid) {
            alert("입력한 정보를 다시 확인해주세요.");
            return;
        }

        try {
            const res = await axios.put(
                "http://192.168.40.190:8080/members/pwdupdate",
                passwordForm,
                { withCredentials: true }
            );
            if (res.data.success) {
                alert("비밀번호가 성공적으로 변경되었습니다!");
                setPasswordForm({ current_password: "", new_password: "" });
                logout();
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
                "http://192.168.40.190:8080/members/delete",
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
    // 마이페이지 처음 로드 시 회원정보 1회만 불러오기
    useEffect(() => {
        memberInfo();
    }, []);

    // 모드가 "reservation"일 때만 예매내역 다시 불러오기
    useEffect(() => {
        if (mode === "reservation") reservePrint();
    }, [mode]);

    /* ===============================
       렌더링
    =============================== */
    return (
        <div style={{ textAlign: "center", marginBottom: 20 }}>
            <h2
                style={{
                    textAlign: "center",
                    marginBottom: 30,
                    fontSize: "50px",       // ✅ 페이지 타이틀 크게
                    fontWeight: "bold",
                    color: "#111827",
                }}
            >
                마이페이지
            </h2>

            {/* 탭 메뉴 */}
            <div
                style={{
                    marginBottom: "20px",
                    display: "flex",
                    justifyContent: "center",
                    gap: "10px",
                    flexWrap: "wrap",
                }}
            >
                <Button variant="soft" color="success" onClick={() => setMode("reservation")}>
                    예매 내역
                </Button>
                <Button variant="soft" onClick={() => setMode("edit")}>
                    회원정보 수정
                </Button>
                {/* 소셜회원이면 비밀번호 변경 탭 숨김 */}
                {!provider && (
                    <Button onClick={() => setMode("password")}>
                        비밀번호 변경
                    </Button>
                )}
                <Button variant="soft" color="danger" onClick={() => setMode("delete")}>
                    회원 탈퇴
                </Button>
            </div>

            {/* 예매내역 */}
            {mode === "reservation" && (
                <Card variant="outlined" sx={{ maxWidth: 720, mx: "auto" }}>
                    <CardContent>
                        <Stack direction="row" alignItems="center" justifyContent="space-between" sx={{ mb: 1 }}>
                            <Typography level="h4" component="h3">예매 내역</Typography>
                            <Chip variant="soft">총 {reservations?.length || 0}건</Chip>
                        </Stack>
                        <Divider sx={{ mb: 2 }} />

                        {reservations.length === 0 ? (
                            <Alert variant="soft" color="neutral">예매 내역이 없습니다.</Alert>
                        ) : (
                            <JoyTable aria-label="예약 테이블" stripe="odd" border="1" cellPadding="8" style={{ width: "100%", borderCollapse: "collapse", textAlign: "center" }}>
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
                                            <td>{r.game.date} {r.game.time}</td>
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
                            </JoyTable>
                        )}
                    </CardContent>
                </Card>
            )}

            {/* 회원정보 수정 */}
            {mode === "edit" && (
                <Card variant="outlined" sx={{ maxWidth: 720, mx: "auto" }}>
                    <CardContent>
                        <Typography level="h4" component="h3" sx={{ mb: 0.5 }}>회원정보 수정</Typography>
                        <Typography level="body-sm" sx={{ color: "neutral.500", mb: 2 }}>
                            연락처와 이메일 등 기본 정보를 최신 상태로 유지해 주세요.
                        </Typography>
                        <Divider sx={{ mb: 2 }} />

                        <form onSubmit={handleInfoUpdate}>
                            <Stack spacing={2}>
                                <FormControl>
                                    <FormLabel>이름</FormLabel>
                                    <Input
                                        name="mname"
                                        value={form.mname || ""}
                                        onChange={(e) => setForm({ ...form, mname: e.target.value })}
                                        onBlur={(e) => validateField("mname", e.target.value)}
                                        placeholder="이름을 입력하세요"
                                    />
                                    {errors.mname && (
                                        <Typography level="body-sm" color="danger" sx={{ mt: 0.5 }}>
                                            {errors.mname}
                                        </Typography>
                                    )}
                                </FormControl>

                                <FormControl>
                                    <FormLabel>전화번호</FormLabel>
                                    <Input
                                        name="mphone"
                                        value={form.mphone || ""}
                                        onChange={(e) => setForm({ ...form, mphone: e.target.value })}
                                        onBlur={(e) => validateField("mphone", e.target.value)}
                                        placeholder="010-0000-0000"
                                    />
                                    {errors.mphone && (
                                        <Typography level="body-sm" color="danger" sx={{ mt: 0.5 }}>
                                            {errors.mphone}
                                        </Typography>
                                    )}
                                </FormControl>

                                <FormControl>
                                    <FormLabel>이메일</FormLabel>
                                    <Input
                                        name="email"
                                        value={form.email || ""}
                                        onChange={(e) => setForm({ ...form, email: e.target.value })}
                                        onBlur={(e) => validateField("email", e.target.value)}
                                        placeholder="example@mail.com"
                                        disabled={provider} // 소셜회원은 이메일 변경 불가
                                    />
                                    {provider ? (
                                        <Typography level="body-sm" sx={{ mt: 0.5, color: "neutral.500" }}>
                                            ※ 소셜 회원은 이메일 변경이 제한됩니다.
                                        </Typography>
                                    ) : (
                                        <>
                                            {/* 안내 문구 */}
                                            <Typography level="body-sm" sx={{ mt: 0.5, color: "neutral.500" }}>
                                                연락 가능한 이메일을 입력하세요.
                                            </Typography>

                                            {/* 이메일 형식 오류 시 경고 문구 */}
                                            {errors.email && (
                                                <Typography level="body-sm" sx={{ mt: 0.5, color: "danger.500" }}>
                                                    {errors.email}
                                                </Typography>
                                            )}
                                        </>
                                    )}
                                </FormControl>

                                <FormControl>
                                    <FormLabel>선호 선수</FormLabel>
                                    <Select
                                        placeholder="선호 선수를 선택하세요"
                                        value={form.pno || ""}
                                        onChange={(e, newValue) => setForm({ ...form, pno: newValue })}
                                    >
                                        {players.map((p) => (
                                            <Option key={p.pno} value={p.pno}>
                                                {p.name} ({p.teamName} / {p.position})
                                            </Option>
                                        ))}
                                    </Select>
                                </FormControl>

                                <FormControl orientation="horizontal" sx={{ alignItems: "center" }}>
                                    <Checkbox
                                        checked={!!form.exchange}
                                        onChange={(e) => setForm({ ...form, exchange: e.target.checked })}
                                    />
                                    <FormLabel sx={{ ml: 1 }}>교환 여부</FormLabel>
                                </FormControl>
                            </Stack>

                            <CardActions sx={{ mt: 2, justifyContent: "flex-end" }}>
                                <Button variant="outlined" onClick={() => memberInfo()}>다시 불러오기</Button>
                                <Button type="submit" variant="soft">수정하기</Button>
                            </CardActions>
                        </form>
                    </CardContent>
                </Card>
            )}

            {/* 일반회원만 비밀번호 변경 가능 */}
            {!provider && mode === "password" && (
                <Card variant="outlined" sx={{ maxWidth: 720, mx: "auto" }}>
                    <CardContent>
                        <Typography level="h4" component="h3" sx={{ mb: 0.5 }}>비밀번호 변경</Typography>
                        <Typography level="body-sm" sx={{ color: "neutral.500", mb: 2 }}>
                            현재 비밀번호 확인 후 새 비밀번호로 변경합니다.
                        </Typography>
                        <Divider sx={{ mb: 2 }} />

                        <form onSubmit={handlePasswordUpdate}>
                            <Stack spacing={2}>
                                <FormControl>
                                    <FormLabel>현재 비밀번호</FormLabel>
                                    <Input
                                        type="password"
                                        name="current_password"
                                        value={passwordForm.current_password}
                                        onChange={(e) =>
                                            setPasswordForm({ ...passwordForm, current_password: e.target.value })
                                        }
                                        placeholder="현재 비밀번호를 입력하세요"
                                    />
                                    {errors.current_password && (
                                        <Typography level="body-sm" color="danger" sx={{ mt: 0.5 }}>
                                            {errors.current_password}
                                        </Typography>
                                    )}

                                </FormControl>

                                <FormControl>
                                    <FormLabel>새 비밀번호</FormLabel>
                                    <Input
                                        type="password"
                                        name="new_password"
                                        value={passwordForm.new_password}
                                        onChange={(e) =>
                                            setPasswordForm({ ...passwordForm, new_password: e.target.value })
                                        }
                                        onBlur={(e) => validateField("new_password", e.target.value)}
                                        placeholder="비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다."
                                    />
                                    <Typography level="body-sm" sx={{ mt: 0.5, color: "danger.500" }}>
                                        {errors.new_password}
                                    </Typography>
                                </FormControl>
                            </Stack>

                            <CardActions sx={{ mt: 2, justifyContent: "space-between" }}>
                                <Button variant="outlined" onClick={() => setPasswordForm({ current_password: "", new_password: "" })}>
                                    초기화
                                </Button>
                                <Button type="submit" color="primary">변경하기</Button>
                            </CardActions>
                        </form>
                    </CardContent>
                </Card>
            )}

            {/* 회원 탈퇴 */}
            {mode === "delete" && (
                <Card variant="outlined" color="danger" sx={{ maxWidth: 720, mx: "auto" }}>
                    <CardContent>
                        <Typography level="h4" component="h3" sx={{ mb: 1, color: "danger.600" }}>
                            회원 탈퇴
                        </Typography>

                        <Alert variant="soft" color="warning" sx={{ mb: 2 }}>
                            탈퇴 시 계정 정보 및 일부 데이터는 복구가 불가합니다. 진행 전 꼭 확인해 주세요.
                        </Alert>
                        <Divider sx={{ mb: 2 }} />

                        <form onSubmit={handleDelete}>
                            {/* 소셜회원이면 비밀번호 입력칸 제거 */}
                            {!provider && (
                                <FormControl sx={{ mb: 2 }}>
                                    <FormLabel>비밀번호 확인</FormLabel>
                                    <Input
                                        type="password"
                                        name="password_hash"
                                        value={deleteForm.password_hash}
                                        onChange={(e) =>
                                            setDeleteForm({ ...deleteForm, password_hash: e.target.value })
                                        }
                                        placeholder="비밀번호를 입력하세요"
                                    />
                                </FormControl>
                            )}

                            <CardActions sx={{ justifyContent: "space-between" }}>
                                <Button variant="outlined" color="neutral" onClick={() => setMode("reservation")}>
                                    돌아가기
                                </Button>
                                <Button type="submit" color="danger">탈퇴하기</Button>
                            </CardActions>
                        </form>
                    </CardContent>
                </Card>
            )}
        </div>
    );

}