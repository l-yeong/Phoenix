package phoenix.model.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import phoenix.model.dto.ReservationExchangesDto;

@Mapper
public interface ReservationExchangeMapper {

    /**
     * 교환성공 데이터 db에 저장
     *
     * @param dto 교환데이터
     * @return true : 성공 , false : 실패
     */
    @Insert("insert into reservation_exchanges(from_rno , to_rno , status , requested_at , responded_at)" +
            " values(#{dto.getFrom_rno},#{dto.getTo_rno},#{dto.getStatus},#{dto.getRequested_at},#{dto.getResponded_at})")
    public boolean changeAdd(ReservationExchangesDto dto);
}//inter end
