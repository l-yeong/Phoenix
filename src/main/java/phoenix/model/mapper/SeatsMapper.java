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
        // 끝

        @Select("SELECT * FROM seats WHERE zno = (SELECT s.zno FROM reservations r INNER JOIN seats s ON s.sno = r.sno WHERE r.rno = #{rno});")
        List<SeatDto> seatPrint(int rno);

}       // class end
