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

        @Select("select * from seats")
        List<SeatDto> seatPrint();

}       // class end
