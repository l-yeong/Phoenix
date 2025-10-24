package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.TicketsDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketsMapper {
    // rno 중복티켓 있는지 조회(중복방지)
    @Select("SELECT ticket_code FROM tickets WHERE rno = #{rno} LIMIT 1;")
    String findTicketdedupe(@Param("rno") int rno);

    // 티켓발급 (ticket_code 에는 QR 이미지 경로)
    @Insert(" INSERT INTO tickets (rno, ticket_code, valid, price, ticket_uuid) " +
            " VALUES (#{rno}, #{ticket_code}, #{valid}, #{price},#{ticket_uuid})")
    int ticketWrite(TicketsDto dto);

    // QR정보
    @Select("SELECT r.mno, r.rno, r.status AS reservation_status, t.ticket_code FROM reservations r "+
            " LEFT JOIN tickets t ON t.rno = r.rno WHERE r.rno = #{rno}")
    Map<String, Object> ticketPrint(@Param("rno") int rno);

    /*==========================================================================================================================*/
    // 회원별 티켓 코드 목록
    @Select(" SELECT t.tno,t.rno,t.ticket_code, DATE_FORMAT(t.issued_at, '%Y-%m-%d %H:%i:%s') AS issued_at, " +
            " t.valid, t.price, r.gno FROM tickets t JOIN reservations r ON t.rno = r.rno " +
            " WHERE r.mno = #{mno} AND t.rno = #{rno} ORDER BY t.issued_at DESC")
    List<Map<String, Object>> findPayloads(@Param("mno") int mno, @Param("rno") long rno);

    //지난 경기 티켓 무효화 valid->false 및 ticket_code null  변경
    @Update(" UPDATE tickets t JOIN reservations r ON r.rno = t.rno " +
            " SET t.valid = 0 WHERE r.gno IN (${gnoList}) " +
            " AND (t.valid = 1)")
    int formerGame(@Param("gnoList") String gnoList);


    // uuid로 바로 예매표시용 정보 조회
    @Select("SELECT m.mname AS mname, z.zname AS zname, s.seatName AS seat_no,z.price AS seat_price, t.valid AS valid " +
            " FROM tickets t JOIN reservations r ON t.rno = r.rno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno " +
            " JOIN members m ON r.mno = m.mno WHERE t.ticket_uuid =#{uuid} LIMIT 1;")
    Map<String, Object> ticketUuidInfo(@Param("uuid") String uuid);

    // QR 스캐너 조회
    @Select("SELECT t.ticket_uuid, t.valid FROM tickets t WHERE ticket_uuid=#{uuid}")
    Map<String, Object> qrScan(@Param("uuid") String uuid);

    // QR 소진(유효→사용됨)
    @Update("UPDATE tickets SET valid=0 WHERE ticket_uuid=#{uuid} and valid=1")
    int qrScanInfoUpdate(@Param("uuid") String uuid);

    //관리자 페이지
    /**
     * 관리자 QR 사용 기록 전체 조회
     * - 모든 티켓 목록을 연락처 포함으로 조회
     */
    @Select("SELECT m.mname AS mname, m.mphone AS mphone, z.zname AS zname, s.seatName AS seat_no, "+
            " CONCAT(z.zname, ' ', s.seatName) AS seat_label, z.price AS seat_price, "+
            " r.status AS reservation_status, t.valid AS valid FROM tickets t JOIN reservations r ON r.rno = t.rno "+
            " JOIN members m ON r.mno = m.mno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno "+
            " ORDER BY t.ticket_code DESC")
    List<Map<String, Object>> adminScanLog();


}//inter end
