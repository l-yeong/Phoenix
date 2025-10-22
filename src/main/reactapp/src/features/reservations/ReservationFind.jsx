import axios from "axios";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import TicketQR from "../tickets/TicketQR";

export default function reservationFind( props ){
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
    const reserveCancle = async() => {
        try{
            const response = await axios.put(`http://localhost:8080/reserve/cancle?rno=${rno}`);
            if(response.data.status == 200){
                alert('예매를 취소 하였습니다');
            }else{
                alert('예매 취소를 실패하였습니다');
            }// func end
        }catch(e){
            console.log(e);
        }// try end
    }// func end

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
            const response = await axios.post(`http://localhost:8080/seat/accept?rno=${fromRno}`);
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

    // 컴포넌트 처음 랜더링시 호출
    useEffect( () => {
        reserveInfo();
        getAllRequest();
    },[rno]);

    return (
        <>
        <h2> 예매 상세내역 </h2>
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
                    <div style={{ margin: "20px 0" }}>
                      <TicketQR rno={parseInt(rno, 10)} />
                    </div>
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
                <button disabled={!cancel}>좌석교환</button>
                <button onClick={reserveCancle} disabled={!cancel}>예매취소</button>
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