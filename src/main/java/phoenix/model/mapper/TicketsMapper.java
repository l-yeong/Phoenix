package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.TicketsDto;

import java.util.List;
import java.util.Map;

@Mapper
public interface TicketsMapper {

    /**
     * rno 기준으로 이미 발급된 티켓이 있는지 확인 (중복 발급 방지)
     *
     * @param rno 예매 고유번호
     */
    @Select("SELECT ticket_code FROM tickets WHERE rno = #{rno} LIMIT 1;")
    String findTicketdedupe(@Param("rno") int rno);

    /**
     * 티켓 발급
     * - ticket_code에는 생성된 QR 이미지 경로가 저장됨
     *
     * @param dto 티켓 정보 DTO
     */
    @Insert(" INSERT INTO tickets (rno, ticket_code, valid, price, ticket_uuid) " +
            " VALUES (#{rno}, #{ticket_code}, #{valid}, #{price},#{ticket_uuid})")
    int ticketWrite(TicketsDto dto);

    /**
     * 예매번호(rno)에 해당하는 티켓 및 예약 상세 조회
     *
     * @param rno 예매 고유번호
     */
    @Select("SELECT r.mno, r.rno, r.status AS reservation_status, t.ticket_code FROM reservations r "+
            " LEFT JOIN tickets t ON t.rno = r.rno WHERE r.rno = #{rno}")
    Map<String, Object> ticketPrint(@Param("rno") int rno);

    /**
     * 회원별 티켓 목록 조회
     * - 회원(mno)과 예매번호(rno)를 기준으로 해당 티켓 내역을 조회
     *
     * @param mno 회원 고유번호
     * @param rno 예매 고유번호
     */
    @Select(" SELECT t.tno,t.rno,t.ticket_code, DATE_FORMAT(t.issued_at, '%Y-%m-%d %H:%i:%s') AS issued_at, " +
            " t.valid, t.price, r.gno FROM tickets t JOIN reservations r ON t.rno = r.rno " +
            " WHERE r.mno = #{mno} AND t.rno = #{rno} ORDER BY t.issued_at DESC")
    List<Map<String, Object>> findPayloads(@Param("mno") int mno, @Param("rno") long rno);

    // 티켓 취소
    @Update("UPDATE tickets SET valid = 0 WHERE rno = #{rno}")
    boolean ticketCancel(int rno);

    /**
     * 지난 경기 티켓 무효화 처리
     * - 유효(valid=1) 상태의 티켓을 무효(valid=0)로 변경
     *
     * @param gnoList 경기번호 목록
     */
    @Update(" UPDATE tickets t JOIN reservations r ON r.rno = t.rno " +
            " SET t.valid = 0 WHERE r.gno IN (#{gnoList}) " +
            " AND (t.valid = 1)")
    int formerGame(@Param("gnoList") String gnoList);


    /**
     * QR UUID로 예매표시용 상세 정보 조회
     * - 회원명, 구역명, 좌석명, 좌석가격, 유효상태 등 반환
     */
    @Select("SELECT m.mname AS mname, z.zname AS zname, s.seatName AS seat_no,z.price AS seat_price, t.valid AS valid " +
            " FROM tickets t JOIN reservations r ON t.rno = r.rno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno " +
            " JOIN members m ON r.mno = m.mno WHERE t.ticket_uuid =#{uuid} LIMIT 1;")
    Map<String, Object> ticketUuidInfo(@Param("uuid") String uuid);


    /**
     * QR 스캐너로 UUID 조회
     * - 해당 QR의 유효상태(valid) 확인용
     *
     * @param uuid 티켓 UUID
     */
    @Select("SELECT t.ticket_uuid, t.valid FROM tickets t WHERE ticket_uuid=#{uuid}")
    Map<String, Object> qrScan(@Param("uuid") String uuid);

    /**
     * QR 스캔 후 유효 상태 변경
     * - valid=1 → valid=0 으로 변경 (1회 사용)
     *
     * @param uuid 티켓 UUID
     */
    @Update("UPDATE tickets SET valid=0 WHERE ticket_uuid=#{uuid} and valid=1")
    int qrScanInfoUpdate(@Param("uuid") String uuid);

    /**
     * 관리자 페이지용 QR 사용 이력 전체 조회
     * - 모든 티켓 목록을 회원 정보(이름, 연락처) 포함으로 조회
     *
     */
    @Select("SELECT m.mname AS mname, m.mphone AS mphone, z.zname AS zname, s.seatName AS seat_no, "+
            " CONCAT(z.zname, ' ', s.seatName) AS seat_label, z.price AS seat_price, "+
            " r.status AS reservation_status, t.valid AS valid FROM tickets t JOIN reservations r ON r.rno = t.rno "+
            " JOIN members m ON r.mno = m.mno JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno "+
            " ORDER BY t.ticket_code DESC")
    List<Map<String, Object>> adminScanLog();

    /**
     * 삭제 대상 QR 이미지 조회
     * - 이미 사용(valid=0)된 티켓 중 이미지 경로 존재하는 항목
     *
     * @param limit 조회할 최대 개수
     */
    @Select("SELECT ticket_code FROM tickets WHERE valid=0 AND ticket_code IS NOT NULL LIMIT #{limit}")
    List<String> QRImgDelete (@Param("limit")int limit);


}//inter end
