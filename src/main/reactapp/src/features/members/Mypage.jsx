export default function Mypage( props ){
    // [1] 예매내역 전체조회
    const reservePrint = async() => {
        
        try{
            const response = await axios.get("http://localhost:8080/reserve/print");
            console.log(response.data);
        }catch(e){
            console.log(e);
        }// try end
    }// func end
    return (
        <>
        
        </>
    )
}// func end