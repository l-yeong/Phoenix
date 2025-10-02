package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationsService {
    private final ReservationMapper reservationMapper;

    /**
     * 예매내역조회
     *
     * @param mno
     * @return List<ReservationsDto>
     */
    public List<ReservationsDto> reservePrint(int mno){
        List<ReservationsDto> list = reservationMapper.reservePrint(mno);
        return list;
    }// func end

    /**
     * 예매내역 상세조회
     *
     * @param mno
     * @param rno
     * @return ReservationsDto
     */
    public ReservationsDto reserveInfo(int mno , int rno){
        return reservationMapper.reserveInfo(mno,rno);
    }// func end

    /**
     * 예매내역 수정
     *
     * @param sno
     * @param rno
     * @param mno
     * @return boolean
     */
    public boolean reserveUpdate(int sno , int rno , int mno){
        return reservationMapper.reserveUpdate(sno, rno , mno);
    }// func end

    /**
     * 예매취소
     *
     * @param rno
     * @param mno
     * @return boolean
     */
    public boolean reserveCancle(int rno , int mno){
        return reservationMapper.reserveCancel(rno, mno);
    }// func end
}//func end
