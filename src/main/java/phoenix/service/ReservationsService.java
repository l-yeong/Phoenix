package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import phoenix.model.dto.GameDto;
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
    private final SeatLockService seatLockService;

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
            GameDto gameData = fileService.getGame(dto.getGno());
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
        GameDto gameMap = fileService.getGame(dto.getGno());
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
    @Transactional(rollbackFor = Exception.class)
    public boolean reserveCancle(int rno, int mno) {
        // 0) 대상 예약 조회
        ReservationsDto dto = reservationMapper.reserveInfo(rno);
        if (dto == null || dto.getMno() != mno) return false;
        if (!"reserved".equalsIgnoreCase(dto.getStatus())) return false; // 이미 취소/기타 상태면 중단

        int gno = dto.getGno();
        int sno = dto.getSno();

        // 1) DB 상태 전환 (reserved → cancelled)
        boolean check1 = reservationMapper.reserveCancel(rno, mno);
        if (!check1) return false; // 조건 불일치(동시성 등) 시 중단

        // 2) 티켓 취소
        boolean cancelTicket = ticketsService.ticketCancel(rno);
        if (!cancelTicket) {
            // DB는 롤백되고 Redis도 손대지 않음(정합성 보장)
            throw new IllegalStateException("티켓 취소 실패");
        }

        // 3) 커밋 후 Redis 갱신
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            // 커밋완료후 실행되늰 거임. 상속받은 거 TransactionSynchronizationManager 인터페이스에서
            // TransactionSynchronization 콜백 메소드를 구현한거임.
            @Override public void afterCommit() {
                // SOLD 해제 + 유저 카운터 감소
                System.out.println("트렌젝션 레디스 확인용 !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                seatLockService.onReservationCancelled(mno, gno, sno);
            }
        });

        return true;
    }

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
