package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.SeatDto;

import java.util.List;
import java.util.Map;

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

        /* ===== 무결성/보안 체크 ===== */
        // sno가 실제 존재하는가
        @Select("""
            SELECT EXISTS(
                SELECT 1 FROM seats
                WHERE sno = #{sno}
            )
            """)
        boolean existsSeatBySno(@Param("sno") int sno);

        // sno가 해당 zno에 속하는가 (클라 변조 방지)
        @Select("""
            SELECT EXISTS(
                SELECT 1 FROM seats
                WHERE sno = #{sno}
                  AND zno = #{zno}
            )
            """)
        boolean existsSeatInZone(@Param("zno") int zno, @Param("sno") int sno);

        /** zno 존재 여부 */
        @Select("""
            SELECT EXISTS(
              SELECT 1 FROM zones WHERE zno = #{zno}
            )
            """)
        boolean existsZone(@Param("zno") int zno);

        /** 특정 존의 좌석 목록 (정렬 보장) */
        @Select("""
            SELECT
              s.sno AS sno,
              s.seatName AS seatName
            FROM seats s
            WHERE s.zno = #{zno}
            ORDER BY 
              LEFT(s.seatName, 1),                 -- 행(A/B/C)
              CAST(SUBSTRING(s.seatName, 2) AS UNSIGNED)  -- 열(1..10)
            """)
        List<Map<String, Object>> findSeatsByZone(@Param("zno") int zno);

        @Select("SELECT * FROM seats WHERE zno = (SELECT s.zno FROM reservations r INNER JOIN seats s ON s.sno = r.sno WHERE r.rno = #{rno});")
        List<SeatDto> seatPrint(int rno);
}
