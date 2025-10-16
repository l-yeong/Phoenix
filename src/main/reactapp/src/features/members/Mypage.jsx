import { useEffect, useState } from "react";

export default function Mypage( props ){
    // 예매내역 상태
    const [ reservations , setReservations ] = useState([]);
        
    // [1] 예매내역 전체조회 ( 회원번호 쿼리스트링으로 전달 )
    const reservePrint = async() => {
        
        try{
            const response = await axios.get("http://localhost:8080/reserve/print");
            console.log(response.data);
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    // [2] 컴포넌트 처음 렌더링 때 호출
    useEffect( () => {
        reservePrint();
    },[]); 
    // ( 예매내역에 경기 정보랑 경기날짜 시간 , 좌석 )
    return (
        <>
        <h2> 예매 내역 </h2>
        {reservations.length == 0 ? (
            <p> 예매 내역이 없습니다. </p>
        ) : (
            <table>
                <thead>
                    <tr>
                        <th></th>
                    </tr>
                </thead>
                <tbody>
                    {reservations.map( (r) => {
                        <tr key={r.rno}>
                            <td></td>
                        </tr>
                    })}
                </tbody>
            </table>
        )}
        </>
    )
}// func end