package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;


@Mapper
public interface SeatsMapper {

        /**
         * [좌석 + 예매 상태 조회]
         * - gno 기준으로 해당 경기의 모든 좌석을 가져온다.
         * - reservations에 존재하고 status='reserved'면 SOLD.
         */
        @Select({
                "SELECT",
                "  s.sno AS sno,",
                "  s.seatName AS seatName,",
                "  s.zno AS zno,",
                "  CASE",
                "    WHEN r.status = 'reserved' THEN 1",
                "    ELSE 0",
                "  END AS is_sold",
                "FROM seats s",
                "LEFT JOIN reservations r",
                "  ON s.sno = r.sno AND r.gno = #{gno}",
                "ORDER BY s.zno, s.seatName"
        })
        List<Map<String, Object>> getSeatsWithReservationStatus(int gno);
}//inter end
