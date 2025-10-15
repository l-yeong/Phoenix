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

    // DB에 저장될 QR 문자열
    @Insert(" INSERT INTO tickets (rno, ticket_code, price, valid) VALUES (#{rno}, #{ticket_code}, #{price}, #{valid})")
    @Options(useGeneratedKeys = true, keyProperty = "tno")
    int ticketWrite(TicketsDto dto);

    // 프론트 QR코드 생성하는 payload 조회 (qrcode.react 렌더링용)
    @Select("SELECT t.ticket_code FROM tickets t JOIN reservations r ON t.rno = r.rno JOIN members m ON r.mno = m.mno "
            +" WHERE m.mno = #{mno} ORDER BY t.tno DESC")
    List<String>findPayloads(@Param("mno")int mno);

    // QR코드 (이름/구역/좌석/사용여부)정보 출력
    @Select("SELECT m.mname AS name,z.zname AS zone,s.seat_no AS seat,t.valid  AS valid FROM tickets t JOIN reservations r ON t.rno = r.rno " +
            " JOIN members m ON r.mno = m.mno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno WHERE t.ticket_code = #{ticket_code}")
    Map<String,Object> findPayloadsInfo(@Param("ticket_code")String ticket_code);


}//inter end
