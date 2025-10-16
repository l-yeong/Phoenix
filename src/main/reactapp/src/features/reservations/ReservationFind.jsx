export default function reservationFind( props ){

    // [1] 예매 상세내역 조회
    const reserveInfo = async () => {
        try{
            const response = await axios.get("http://localhost:8080/reserve/info");
            
        }catch(e){
            console.log(e);
        }// try end
    }// func end

    return (
        <>
        
        </>
    )
}// func end