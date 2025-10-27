import axios from "axios";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import * as React from 'react';
import {
  List,
  ListItem,
  Card,
  CardContent,
  CardActions,
  Typography,
  Divider,
  Button,
  Stack,
  Box,
} from "@mui/joy";import Modal from '@mui/joy/Modal';
import ModalClose from '@mui/joy/ModalClose';
import Sheet from '@mui/joy/Sheet';
import "../../styles/zone-seats.css";
import TicketQR from "../tickets/TicketQR";

export default function reservationFind( props ){
    // [*] 모달 상태 관리
    const [open, setOpen] = React.useState(false);
    // [*] 교환 가능한 좌석 목록
    const [changeSeat , setChangeSeat] = useState([]);
    // [*] 전체좌석목록
    const [ seatList , setSeatList ] = useState([]);
    // [*] 예매 상태 관리
    const [ reservation , setReservation ] = useState(null);
    // [*] 교환요청목록 상태관리
    const [ exchange , setExchange ] = useState([]);
    // [*] 예매번호 URL에서 추출
    const {rno} = useParams();
    // [*] 요청 받은목록 체크
    const isEmpty = !exchange || exchange.length === 0;
    // 현재시간
    const now = new Date();
    // [1] 예매 상세내역 조회
    const reserveInfo = async () => {
        try{            
            const response = await axios.get(`http://localhost:8080/reserve/info?rno=${rno}`);
            setReservation(response.data);
            console.log(response.data);
            // 경기날짜+시간
            const gameDate = reservation?.game ? new Date(`${reservation.game.date}T${reservation.game.time}`) : null;
            // 취소 여부 체크
            const cancel = gameDate ? now < gameDate : false;
        }catch(e){
            console.log(e);
        }// try end
    }// func end
      
    // [2] 예매 취소 
    const reserveCancle = async () => {
        try {
            const response = await axios.put(`http://localhost:8080/reserve/cancle?rno=${rno}`, {}, { withCredentials: true });
            console.log(response.data);
            if (response.data) {
                alert('예매를 취소 하였습니다');
            } else {
                alert('예매 취소를 실패하였습니다');
            }
        } catch (e) {
            console.log(e);
        }
    };

    // [3] 교환 요청 받은 목록
    const getAllRequest = async() => {
        try{
            const response = await axios.get(`http://localhost:8080/seat/find?rno=${rno}`, {} , {withCredentials : true});
            if(response.data == null){
                return;
            }else{
                setExchange(response.data);
                console.log(response.data);
            }// if end
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [4] 교환요청 수락
    const acceptChange = async (from_rno) => {
        try{
            const response = await axios.post(`http://localhost:8080/seat/accept?rno=${from_rno}`,{} , { withCredentials: true });
            console.log(response.data);
            if(response.data){
                alert('좌석이 교환되었습니다');
                getAllRequest();
            }else{
                alert('좌석교환 실패하였습니다');
            }// if end
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [5] 교환요청 거절
    const rejectChange = async ( from_rno ) => {
        try{
            const response = await axios.delete(`http://localhost:8080/seat/reject?rno=${from_rno}` , {} , { withCredentials: true });
            console.log(response.data);
            if(response.data){
                alert('좌석교환을 거절하였습니다.');
                getAllRequest();
            }else{
                alert('교환 거절 실패하였습니다.');
            }// if end
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [6] 좌석 교환 요청
    const saveRequest = async(toSno , to_rno) => {
        const check = confirm('교환요청을 보내시겠습니까?');
        if(check){
            try{
                const obj = { from_rno : Number(rno) , to_rno : to_rno , toSno : toSno }
                console.log(obj);
                const response = await axios.post(`http://localhost:8080/seat/change`,obj , { withCredentials: true });
                console.log(response.data);
                if(response.data == 1){
                    alert('좌석교환 신청을 완료하였습니다.');
                }else if(response.data == 2){
                    alert('현재 요청이 많아 잠시후 다시 시도해주세요.');
                }else{
                    alert('좌석교환 신청을 실패하였습니다.');
                }// if end
            }catch(e){ console.log(e); }
        }else{
            return
        }// if end
    }// func end

    // [7] 모달오픈 클릭이벤트
    const openModalEvent = async () => {
        try{
            const response = await axios.get(`http://localhost:8080/reserve/possible?rno=${rno}`);
            setChangeSeat(response.data);
            setOpen(true);
        }catch(e){
            console.log(e);
            alert('좌석 정보를 불러오지 못했습니다.');
        }
    }// func end

    // [8] 전체좌석 가져오기
    const seatPrint = async () => {
        try{
            const response = await axios.get(`http://localhost:8080/seat/print?rno=${rno}`);
            setSeatList(response.data);
        }catch(e){ console.log(e); }
    }// func end

    // 컴포넌트 처음 랜더링시 호출
    useEffect( () => {
        reserveInfo();
        getAllRequest();
        seatPrint();
    },[rno]);

    // [*]
    function InfoItem({ label, value }) {
        return (
            <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography level="body-sm" color="neutral">
                {label}
            </Typography>
            <Typography level="body-sm" fontWeight="md">
                {value}
            </Typography>
            </Box>
        );
    }// func end

    return (
        <>
        <h2 style={{marginLeft: "12px" , paddingLeft: "24px"}}> 예매 상세내역 </h2>
        <h2> 예매 상세내역 </h2>
                             <div style={{ margin: "20px 0" }}>
                                        <TicketQR />
                              </div>
        {!reservation || !reservation.reservation || !reservation.game ? (
            <p> 예매 정보를 불러오는중... </p>
        ) : (
            (() => {
            // 렌더링 시점에 계산
            const now = new Date();
            const gameDate = new Date(`${reservation.game.date}T${reservation.game.time}`);
            const cancel = now < gameDate;

            return (
                <div>

                     <Card
                        variant="outlined"
                        sx={{
                            width: 600,             // 🔹 카드 너비 크게 (기존 400 → 600)
                            height: "auto",         // 🔹 높이는 내용에 따라 자동
                            ml: 4,                  // 🔹 왼쪽 여백 (margin-left)
                            mt: 3,                  // 🔹 위쪽 여백
                            p: 3,                   // 🔹 안쪽 여백 넉넉하게
                            borderRadius: "lg",
                            boxShadow: "sm",
                            bgcolor: "background.body",
                        }}
                        >
                        <Typography level="title-lg" textAlign="center" mb={1}>
                            예매 정보
                        </Typography>
                        <Divider inset="none" />

                        <CardContent>
                            <Stack spacing={1.2} mt={1}>
                            <InfoItem label="예매번호" value={reservation.reservation?.rno ?? "-"} />
                            <InfoItem label="좌석번호" value={reservation.reservation?.sno ?? "-"} />
                            <InfoItem label="홈팀" value={reservation.game?.homeTeam ?? "-"} />
                            <InfoItem label="홈팀 선발투수" value={reservation.game?.homePitcher ?? "-"} />
                            <InfoItem label="어웨이팀" value={reservation.game?.awayTeam ?? "-"} />
                            <InfoItem label="어웨이팀 선발투수" value={reservation.game?.awayPitcher ?? "-"} />
                            <InfoItem label="경기날짜" value={reservation.game?.date ?? "-"} />
                            <InfoItem label="경기시간" value={reservation.game?.time ?? "-"} />
                            <InfoItem
                                label="취소가능여부"
                                value={
                                <Typography
                                    level="body-sm"
                                    fontWeight="lg"
                                    color={cancel ? "success" : "danger"}
                                >
                                    {cancel ? "취소 가능" : "취소 불가"}
                                </Typography>
                                }
                            />
                            </Stack>
                        </CardContent>

                        <Divider inset="none" />

                        <CardActions sx={{ justifyContent: "flex-end", pt: 1 }}>
                            <Button
                            variant="outlined"
                            color="primary"
                            disabled={!cancel}
                            onClick={openModalEvent}
                            >
                            좌석교환
                            </Button>
                            <Button
                            variant="outlined"
                            color="danger"
                            disabled={!cancel || reservation.reservation.status === "cancelled"}
                            onClick={reserveCancle}
                            >
                            예매취소
                            </Button>
                        </CardActions>
                        </Card>
                    <Modal
                        aria-labelledby="modal-title"
                        aria-describedby="modal-desc"
                        open={open}
                        onClose={() => setOpen(false)}
                        sx={{ display: "flex", justifyContent: "center", alignItems: "center" }}
                        >
                        <Sheet
                            variant="soft"
                            color="neutral"
                            sx={{
                            width: 520,
                            borderRadius: "lg",
                            p: 4,
                            boxShadow: "xl",
                            bgcolor: "background.surface",
                            textAlign: "center",
                            }}
                        >
                            <ModalClose variant="plain" sx={{ m: 1 }} />

                            <Typography
                            id="modal-title"
                            level="h4"
                            sx={{ fontWeight: "lg", mb: 2 }}
                            >
                            🎟️ 좌석 교환
                            </Typography>

                            <Typography
                            id="modal-desc"
                            textColor="text.tertiary"
                            sx={{ mb: 3 }}
                            >
                            교환 가능한 좌석을 선택해주세요.
                            </Typography>

                            <Box
                            sx={{
                                display: "grid",
                                gridTemplateColumns: "repeat(5, 1fr)", // 한 줄에 5개씩
                                gap: 1.5,
                                justifyItems: "center",
                                mb: 3,
                            }}
                            className="seat-grid"
                            >
                            {seatList.map((s) => {
                                const seatInfo = changeSeat.find((cs) => cs.sno === s.sno);
                                const to_rno = seatInfo ? seatInfo.rno : undefined;
                                const isAvailable = changeSeat.some((cs) => cs.sno === s.sno);

                                return (
                                <Button
                                    key={s.sno}
                                    onClick={() => saveRequest(s.sno,to_rno)}
                                    disabled={!isAvailable}
                                    variant={isAvailable ? "soft" : "outlined"}
                                    color={isAvailable ? "success" : "neutral"}
                                    sx={{
                                    width: "100%",       // 🔹 셀 너비에 맞춤
                                    maxWidth: 90,        // 최대 폭 제한
                                    height: 50,
                                    fontWeight: "md",
                                    borderRadius: "md",
                                    textTransform: "none",
                                    opacity: isAvailable ? 1 : 0.5,
                                    transition: "all 0.2s ease",
                                    "&:hover": {
                                        transform: isAvailable ? "scale(1.05)" : "none",
                                    },
                                    }}
                                >
                                    {s.seatName}
                                </Button>
                                );
                            })}
                            </Box>

                            <Button
                            fullWidth
                            variant="soft"
                            color="neutral"
                            onClick={() => setOpen(false)}
                            sx={{ mt: 1 }}
                            >
                            닫기
                            </Button>
                        </Sheet>
                        </Modal>
                </div>
            );
            })()
        )}
        <h2 style={{marginLeft: "12px" , paddingLeft: "24px"}}> 교환 요청 받은 목록 </h2>
        <Card
            variant="outlined"
            sx={{
                width: 600,
                mt: 2,
                ml: 4,
                p: 2,
                mb: 6,
                borderRadius: "lg",
                boxShadow: "sm",
            }}
            >
            <CardContent>
                <Typography level="title-md" mb={1}>
                좌석 교환 요청
                </Typography>
                <Divider inset="none" sx={{ mb: 1 }} />

                {isEmpty ? (
                <Typography
                    level="body-sm"
                    color="neutral"
                    sx={{ ml: 2, pl: 2, py: 1 }}
                >
                    교환 요청이 없습니다.
                </Typography>
                ) : (
                <List
                    variant="outlined"
                    sx={{
                    border: "none",
                    pl: 2,
                    ml: 1,
                    gap: 1,
                    }}
                >
                    {exchange.map((ex) => (
                    <ListItem
                        key={ex?.from_rno}
                        sx={{
                        display: "flex",
                        justifyContent: "space-between",
                        alignItems: "center",
                        bgcolor: "background.level1",
                        borderRadius: "md",
                        p: 1.2,
                        }}
                    >
                        <Typography level="body-sm">
                        <strong>{ex?.fromSeat}</strong> 번 좌석에서 교환 요청을 보냈습니다.
                        </Typography>

                        <Box display="flex" gap={1}>
                        <Button
                            size="sm"
                            variant="outlined"
                            color="success"
                            onClick={() => acceptChange(ex?.from_rno)}
                        >
                            수락
                        </Button>
                        <Button
                            size="sm"
                            variant="outlined"
                            color="danger"
                            onClick={() => rejectChange(ex?.from_rno)}
                        >
                            거절
                        </Button>
                        </Box>
                    </ListItem>
                    ))}
                </List>
                )}
            </CardContent>
            </Card>

        </>
    )
}// func end