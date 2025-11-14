package phoenix.model.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.GetMapping;
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
    @Select("select * from reservations where mno = #{mno} order by rno desc" )
    public List<ReservationsDto> reservePrint(int mno);

    /**
     * 예매내역 상세조회
     *
     * @param rno
     * @return ReservationsDto
     */
    @Select("select * from reservations where rno = #{rno}")
    public ReservationsDto reserveInfo(int rno);

    /**
     * 예매내역 수정
     *
     * @param sno
     * @param rno
     * @param mno
     * @return boolean
     */
    @Update("update reservations set sno = #{sno} where rno = #{rno} and mno = #{mno}")
    public boolean reserveUpdate(int sno , int rno , int mno);

    /**
     * 예매 취소
     *
     * @param rno
     * @param mno
     * @return boolean
     */
    @Update("update reservations set status = 'cancelled' where rno = #{rno} and mno = #{mno} ")
    public boolean reserveCancel(int rno , int mno);

    /**
     * 교환신청 가능한 좌석목록 예매정보
     *
     * @param rno
     * @return List<ReservationsDto>
     */
    @Select("select r.* from reservations r inner join seats s on r.sno = s.sno inner join members m on r.mno = m.mno " +
            " where r.gno = (select gno from reservations where rno = #{rno}) and " +
            " s.zno = (select s2.zno from reservations r2 inner join seats s2 on r2.sno = s2.sno where r2.rno = #{rno})\n" +
            "  and r.status = 'reserved' and r.rno != #{rno} and r.mno != #{mno} and m.exchange = true ")
    List<ReservationsDto> seatPossible(int rno,int mno);
}//inter end
