import axios from "axios";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export default function reservationFind( props ){
    // [*] 예매 상태 관리
    const [ reservation , setReservation ] = useState(null);
    // [*] 예매번호 URL에서 추출
    const rno = useParams();
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

    // 컴포넌트 처음 랜더링시 호출
    useEffect( () => {
        reserveInfo();
    },[props.rno]);

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
                </ul>
                <button> 좌석교환 </button> <button> 예매취소 </button>
            </div>
        )}
        </>
    )
}// func end