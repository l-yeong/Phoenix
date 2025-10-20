package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;


@Mapper
public interface SeatsMapper {

        @Select("""
    SELECT r.gno, s.seat_name
    FROM reservations r
    JOIN seats s ON r.sno = s.sno
    GROUP BY r.gno, s.seat_name
    ORDER BY r.gno, s.seat_name
""")
        @MapKey("gno")
        Map<Integer, List<String>> findAllSeatNamesGroupedByGame();


}//inter end
