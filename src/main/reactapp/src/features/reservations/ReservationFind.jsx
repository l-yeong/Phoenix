import axios from "axios";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import * as React from 'react';
import Button from '@mui/joy/Button';
import Modal from '@mui/joy/Modal';
import ModalClose from '@mui/joy/ModalClose';
import Typography from '@mui/joy/Typography';
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
            const response = await axios.get(`http://localhost:8080/seat/find?rno=${rno}`);
            setExchange(response.data);
            console.log(response.data);
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [4] 교환요청 수락
    const acceptChange = async (fromRno) => {
        try{
            const response = await axios.post(`http://localhost:8080/seat/accept?rno=${fromRno}`,{} , { withCredentials: true });
            if(response.data.status == 200){
                alert('좌석이 교환되었습니다');
            }else{
                alert('좌석교환 실패하였습니다');
            }// if end
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [5] 교환요청 거절
    const rejectChange = async (fromRno) => {
        try{
            const response = await axios.delete(`http://localhost:8080/seat/reject?rno=${fromRno}`);
            if(response.data.status == 200){
                alert('좌석교환을 거절하였습니다.');
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

    return (
        <>
        <h2> 예매 상세내역 </h2>
<div style={{ margin: "20px 0", display: "flex", justifyContent: "center" }}>
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
                    <ul>
                        <li>예매번호 : {reservation.reservation?.rno ?? "-"}</li>
                        <li>좌석번호 : {reservation.reservation?.sno ?? "-"}</li>
                        <li>홈팀 : {reservation.game?.homeTeam ?? "-"}</li>
                        <li>홈팀 선발투수 : {reservation.game?.homePitcher ?? "-"}</li>
                        <li>어웨이팀 : {reservation.game?.awayTeam ?? "-"}</li>
                        <li>어웨이팀 선발투수 : {reservation.game?.awayPitcher ?? "-"}</li>
                        <li>경기날짜 : {reservation.game?.date ?? "-"}</li>
                        <li>경기시간 : {reservation.game?.time ?? "-"}</li>
                        <li>취소가능여부 : {cancel ? "취소 가능" : "취소 불가"}</li>
                    </ul>
                    <Button variant="outlined" color="neutral" onClick={openModalEvent} disabled={!cancel}>
                        좌석교환
                    </Button>
                    <Button variant="outlined" color="neutral" onClick={reserveCancle} disabled={!cancel || reservation.reservation.status == 'cancelled'}>
                        예매취소
                    </Button>
                    <Modal
                        aria-labelledby="modal-title"
                        aria-describedby="modal-desc"
                        open={open}
                        onClose={() => setOpen(false)}
                        sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center' }}
                    >
                        <Sheet
                        variant="outlined"
                        sx={{ maxWidth: 500, borderRadius: 'md', p: 3, boxShadow: 'lg' }}
                        >
                        <ModalClose variant="plain" sx={{ m: 1 }} />
                        <Typography
                            component="h2"
                            id="modal-title"
                            level="h4"
                            textColor="inherit"
                            sx={{ fontWeight: 'lg', mb: 1 }}
                        >
                            좌석교환
                        </Typography>
                        <Typography id="modal-desc" textColor="text.tertiary" component="div">
                            <div className="seat-grid">
                                {seatList.map( (s) => {
                                    const seatInfo = changeSeat.find(cs => cs.sno === s.sno); // 같은 좌석번호 검색
                                    const to_rno = seatInfo ? seatInfo.rno : undefined;
                                    return (
                                        <button
                                            key={s.sno}
                                            onClick={() => saveRequest(s.sno, to_rno)} // 클릭 시 좌석번호 전달
                                            disabled={!changeSeat.some(cs => cs.sno === s.sno)} // 가능 좌석만 활성화
                                            className={`seat-chip`}
                                        >
                                            {s.seatName}
                                        </button>

                                    )
                                })}
                            </div>
                        </Typography>
                        </Sheet>
                    </Modal>
                </div>
            );
            })()
        )}
        <h2> 교환 요청 받은 목록 </h2>
        {exchange.length === 0 ? (
            <p>교환 요청이 없습니다.</p>
        ) : (
            <ul>
                {exchange.map( (ex) => {
                    return ( 
                    <li key={ex.fromRno}>{ex.fromSeat} 번 좌석에서 좌석 교환 요청을 보냈습니다. 
                    <button onClick={ (e) => {acceptChange(ex.fromRno)} }>수락</button> <button onClick={(e) => {rejectChange(ex.fromRno)} }>거절</button></li>
                )})}
            </ul>
        )
        }

        </>
    )
}// func end