package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;


@Mapper
public interface SeatsMapper {

        @Select("""
        SELECT r.gno, s.seatName
        FROM reservations r
        JOIN seats s ON r.sno = s.sno
        WHERE r.status = 'reserved'
        GROUP BY r.gno, s.seatName
        ORDER BY r.gno, s.seatName
    """)
        @MapKey("gno")
        Map<Integer, List<String>> findAllSeatNamesGroupedByGame();

}//inter end
