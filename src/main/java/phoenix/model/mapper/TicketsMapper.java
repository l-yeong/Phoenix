package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.TicketsDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketsMapper {

    // 예약 상태 확인
    @Select("SELECT r.status AS reservation_status, z.price  AS seat_price, "+
        " m.mname  AS mname,z.zname  AS zname, s.seat_no AS seat_no "+
        " FROM reservations r JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno "+
        " JOIN members m ON r.mno = m.mno WHERE r.rno = #{rno} ")
    Map<String,Object> ticketPrint(@Param("rno") int rno);

    // 티켓 QR 이미지 추가
    @Insert(" INSERT INTO tickets (rno, ticket_code, price, valid) VALUES (#{rno}, #{ticket_code}, #{price}, #{valid})")
    @Options(useGeneratedKeys = true, keyProperty = "tno")
    int ticketWrite(TicketsDto dto);

    // payload 목록 조회(프론트에서 qrcode 패키지 사용될것)
    @Select("SELECT t.qr_payload FROM ticket t JOIN reservations r ON t.rno = r.rno JOIN members m ON r.mno = m.mno"
            +"WHERE m.mno = #{mno} ORDER BY t.tno DESC")
    List<String>findPayloads(@Param("mno")int mno);

}//inter end
