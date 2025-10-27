package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.dto.ReservationsDto;
import phoenix.model.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationsService {
    private final ReservationMapper reservationMapper;
    private final FileService fileService;
    private final TicketsService ticketsService;

    /**
     * 예매내역조회
     *
     * @param mno
     * @return List<Map<String,Object>>
     */
    public List<Map<String,Object>> reservePrint(int mno){
        List<ReservationsDto> list = reservationMapper.reservePrint(mno);
        List<Map<String,Object>> result = new ArrayList<>();
        for (ReservationsDto dto : list){
            Map<String ,Object> map = new HashMap<>();
            map.put("reservation",dto);
            // csv 파일
            Map<String,String> gameData = fileService.getGame(dto.getGno());
            map.put("game",gameData);
            result.add(map);
        }// for end
        return result;
    }// func end

    /**
     * 예매내역 상세조회
     *
     * @param rno
     * @return  Map<String ,Object>
     */
    public  Map<String ,Object> reserveInfo(int rno){
        Map<String ,Object> map = new HashMap<>();
        ReservationsDto dto = reservationMapper.reserveInfo(rno);
        Map<String ,String> gameMap = fileService.getGame(dto.getGno());
        map.put("reservation",dto);
        map.put("game",gameMap);
        return map;
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
        boolean cancelReservation = reservationMapper.reserveCancel(rno,mno);
        boolean cancelTicket = ticketsService.ticketCancel(rno);
        return cancelReservation&&cancelTicket;

    }// func end

    /**
     * 교환신청 가능한 좌석목록 예매정보
     *
     * @param rno
     * @return List<ReservationsDto>
     */
    public List<ReservationsDto> seatPossible(int rno,int mno){
        return reservationMapper.seatPossible(rno,mno);
    }// func end
}//func end
