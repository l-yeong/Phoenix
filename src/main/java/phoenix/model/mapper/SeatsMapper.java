package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.ReservationsDto;
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

        // 좌석 확정시 reservation 만드는 로직
        @Insert("""
        INSERT INTO reservations (mno, sno, gno, status)
        VALUES (#{mno}, #{sno}, #{gno}, 'reserved')
        """ )
        @Options(useGeneratedKeys = true, keyProperty = "rno", keyColumn = "rno")
        boolean insertReservation(ReservationsDto dto);



        @Select("SELECT * FROM seats WHERE zno = (SELECT s.zno FROM reservations r INNER JOIN seats s ON s.sno = r.sno WHERE r.rno = #{rno});")
        List<SeatDto> seatPrint(int rno);

}       // class end
