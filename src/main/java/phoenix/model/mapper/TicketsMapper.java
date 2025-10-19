package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.TicketsDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketsMapper {
    // rno 중복티켓 있는지 조회(중복방지)
    @Select("SELECT ticket_code FROM tickets WHERE rno = #{rno} LIMIT 1")
    String findTicketdedupe(@Param("rno") int rno);

    // 티켓발급 (ticket_code 에는 QR 이미지 경로)
    @Insert("INSERT INTO tickets (rno, ticket_code, price, valid) VALUES (#{rno}, #{ticket_code}, #{price}, #{valid})")
    int ticketWrite(TicketsDto dto);

    // 예약기반 정보 조회
    @Select("SELECT r.status AS reservation_status, z.price  AS seat_price, m.mname  AS mname, "+
            " z.zname  AS zname, s.seat_no AS seat_no FROM reservations r JOIN seats s ON r.sno = s.sno "+
            " JOIN zones z ON s.zno = z.zno JOIN members m ON r.mno = m.mno WHERE r.rno = #{rno} ")
    Map<String, Object> ticketPrint(@Param("rno") int rno);

    // 회원별 티켓 코드 목록
    @Select("SELECT t.ticket_code FROM tickets t JOIN reservations r ON t.rno = r.rno "+
            " JOIN members m ON r.mno = m.mno WHERE m.mno = #{mno} ORDER BY t.tno DESC ")
    List<String> findPayloads(@Param("mno") int mno);

    //지난 경기 티켓 무효화 valid->false 변경
    @Update("UPDATE tickets t JOIN reservations r ON r.rno = t.rno SET t.valid = 0 " +
            "WHERE r.gno IN (${gnoList}) AND t.valid = 1")
    int ticketNullify(@Param("gnoList")String gnoList);

    //지난 경기 티켓 QR삭제
    @Update("UPDATE tickets t JOIN reservations r ON r.rno = t.rno " +
            "SET t.ticket_code = NULL WHERE r.gno IN (${gnoList}) " +
            "AND t.ticket_code IS NOT NULL")
    int ticketDelete(@Param("gnoList")String gnoList);

    // QR 스캔 상세 정보
    //@Select(" SELECT m.mname AS name, z.zname AS zone, s.seat_no AS seat, t.valid FROM tickets t JOIN reservations r ON t.rno = r.rno "+
    //       " JOIN members m ON r.mno = m.mno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno WHERE t.ticket_code = #{ticket_code}")
    //Map<String, Object> findPayloadsInfo(@Param("ticket_code") String ticket_code);


}//inter end
