import axios from "axios";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export default function reservationFind( props ){
    // [*] 예매 상태 관리
    const [ reservation , setReservation ] = useState(null);
    // [*] 교환요청목록 상태관리
    const [ exchange , setExchange ] = useState([]);
    // [*] 예매번호 URL에서 추출
    const {rno} = useParams();
    // [1] 예매 상세내역 조회
    const reserveInfo = async () => {
        try{            
            const response = await axios.get(`http://localhost:8080/reserve/info?rno=${rno}`);
            setReservation(response.data);
            console.log(response.data);
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // 현재시간
    const now = new Date();
    // 경기날짜+시간
    const gameDate = reservation ? new Date(`${reservation.game.date}T${reservation.game.time}`) : null;
    // 취소 여부 체크
    const cancel = now < gameDate;

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

    // 컴포넌트 처음 랜더링시 호출
    useEffect( () => {
        reserveInfo();
        getAllRequest();
    },[rno]);

    return (
        <>
        <h2> 예매 상세내역 </h2>
        {!reservation.reservation || !reservation.game ? (
            <p> 예매 정보를 불러오는중... </p>
        ) : (
            <div>
                <ul>
                    <li>예매번호 : {reservation.reservation.rno}</li>
                    <li>좌석번호 : {reservation.reservation.sno}</li>
                    <li>홈팀 : {reservation.game.homeTeam}</li>
                    <li>홈팀 선발투수 : {reservation.game.homePitcher}</li>
                    <li>어웨이팀 : {reservation.game.awayTeam}</li>
                    <li>어웨이팀 선발투수 : {reservation.game.awayPitcher}</li>
                    <li>경기날짜 : {reservation.game.date}</li>
                    <li>경기시간 : {reservation.game.time}</li>
                    <li>취소여부 : {cancel ? "취소 가능" : "취소 불가"}</li>
                </ul>
                <button disabled={!cancel}> 좌석교환 </button> <button onClick={reserveCancle} disabled={!cancel}> 예매취소 </button>
            </div>
        )}
        <h2> 교환 요청 받은 목록 </h2>

        </>
    )
}// func end