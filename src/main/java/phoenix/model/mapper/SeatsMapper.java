// src/main/java/phoenix/model/mapper/SeatsMapper.java
package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.dto.SeatDto;

import java.util.List;

@Mapper
public interface SeatsMapper {

        /* ===== SOLD 복구용 ===== */
        @Select("""
        SELECT DISTINCT gno
        FROM reservations
        WHERE status = 'reserved'
        """)
        List<Integer> findAllGnosHavingReserved();

        @Select("""
        SELECT sno
        FROM reservations
        WHERE gno = #{gno}
          AND status = 'reserved'
        """)
        List<Integer> findReservedSnosByGno(@Param("gno") int gno);

        /* ===== 카운터 복구 (분리) ===== */
        @Select("""
        SELECT mno, gno, COUNT(*) AS count
        FROM reservations
        WHERE status = 'reserved'
          AND channel = 'general'
        GROUP BY mno, gno
        """)
        List<ReservationsDto> findUserReservedCountSummaryGeneral();

        @Select("""
        SELECT mno, gno, COUNT(*) AS count
        FROM reservations
        WHERE status = 'reserved'
          AND channel = 'senior'
        GROUP BY mno, gno
        """)
        List<ReservationsDto> findSeniorReservedCountSummary();

        /* ===== insert (channel 포함) ===== */
        @Insert("""
        INSERT INTO reservations (mno, sno, gno, status, channel)
        VALUES (#{mno}, #{sno}, #{gno}, #{status}, #{channel})
        """)
        @Options(useGeneratedKeys = true, keyProperty = "rno", keyColumn = "rno")
        boolean insertReservationWithChannel(ReservationsDto dto);

        /* ===== 기타 ===== */

        @Select("SELECT * FROM seats WHERE zno = (SELECT s.zno FROM reservations r INNER JOIN seats s ON s.sno = r.sno WHERE r.rno = #{rno});")
        List<SeatDto> seatPrint(int rno);
}
