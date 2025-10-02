package phoenix.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import phoenix.model.dto.ReservationsDto;

import java.util.List;

@Mapper
public interface ReservationMapper {

    /**
     * 예매내역조회
     *
     * @param mno
     * @return List<ReservationsDto>
     */
    @Select("select * from reservations where mno = #{mno}" )
    public List<ReservationsDto> reservePrint(int mno);

    /**
     * 예매내역 상세조회
     *
     * @param mno
     * @param rno
     * @return ReservationsDto
     */
    @Select("select * from reservations where mno = #{mno} and rno = #{rno}")
    public ReservationsDto reserveInfo(int mno , int rno);
}//inter end
