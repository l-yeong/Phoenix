import axios from "axios";
import { useEffect, useState } from "react";

export default function Mypage( props ){
    // 예매내역 상태
    const [ reservations , setReservations ] = useState([]);
        
    // [1] 예매내역 전체조회 ( 회원번호 쿼리스트링으로 전달 )
    const reservePrint = async() => {        
        try{
            const response = await axios.get("http://localhost:8080/reserve/print");
            setReservations(response.data); // 상태 업데이트
            console.log(response.data);
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [2] 컴포넌트 처음 렌더링 때 호출
    useEffect( () => {
        reservePrint();
    },[]); 
    return (
        <>
        <h2> 예매 내역 </h2>
        {reservations.length == 0 ? (
            <p> 예매 내역이 없습니다. </p>
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
                    {reservations.map( (r) => {
                        return (
                            <tr key={r.reservation.rno}>
                                <td><Link to={`/reservationFind/${r.reservation.rno}`}>{r.reservation.rno}</Link></td>
                                <td>{r.reservation.sno}</td>
                                <td>{r.game.homeTeam}</td>
                                <td>{r.game.awayTeam}</td>
                                <td>{r.game.date} {r.game.time}</td>
                                <td>{r.reservation.status === "reserved" ? "예매완료" : r.reservation.status === "cancelled" ? "예매취소" : r.reservation.status}</td>
                            </tr>
                        )})}
                </tbody>
            </table>
        )}
        </>
    )
}// func end