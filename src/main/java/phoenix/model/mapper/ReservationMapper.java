package phoenix.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.web.bind.annotation.PutMapping;
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

    /**
     * 예매내역 수정
     *
     * @param sno
     * @param rno
     * @param mno
     * @return boolean
     */
    @PutMapping("update reservations set sno = #{sno} where rno = #{rno} and mno = #{mno}")
    public boolean reserveUpdate(int sno , int rno , int mno);
}//inter end
