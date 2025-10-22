package phoenix.model.mapper;

import org.apache.ibatis.annotations.*;
import phoenix.model.dto.SeatsDto;

import java.util.List;
import java.util.Map;


@Mapper
public interface SeatsMapper {
        // 전 경기 공통 좌석 템플릿 (좌석명만)
        @Select("""
        SELECT seatName
        FROM seats
        ORDER BY zno, seatName
    """)
        List<String> findAllSeatNames();

        // (선택) 이미 예약된 좌석을 gno별로 보고 싶을 때: 리스트로 받고 서비스에서 groupBy
        @Select("""
        SELECT r.gno AS gno, s.seatName AS seatName
        FROM reservations r
        JOIN seats s ON r.sno = s.sno
        WHERE r.status = 'reserved'
        ORDER BY r.gno, s.seatName
    """)
        List<ReservedSeatRow> findReservedSeatRows();

        class ReservedSeatRow {
                public int gno;
                public String seatName;
        }

        // 전체 좌석 조회
        @Select("select * from seats")
        List<SeatsDto> seatPrint();
}

