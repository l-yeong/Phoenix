package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.service.MembersService;
import phoenix.service.RedisService;
import phoenix.service.ReservationExchangesService;

import java.util.List;

@RequestMapping("/seat")
@RestController
@RequiredArgsConstructor
public class ReservationExchangesController {
    private final ReservationExchangesService reservationexchangesService;
    private final RedisService redisService;
    private final MembersService membersService;

    /**
     * 교환요청 등록
     *
     * @param dto 요청 dto
     * @return int 성공 : 1 , 요청중인사람존재 : 2 , 요청자가 다른좌석에 요청중 : 0
     */
    @PostMapping("/change")
    public ResponseEntity<?> saveRequest(ReservationExchangesDto dto){
        int mno = membersService.getLoginMember().getMno();
        dto.setFrom_mno(mno);
        int result = reservationexchangesService.requestChange(dto);
        return ResponseEntity.ok(result);
    }// func end

    /**
     * 로그인한 회원한테 온 요청목록 조회
     *
     * @param rno 로그인한 회원 얘매번호
     * @return List<ReservationExchangesDto> 요청목록
     */
    @GetMapping("/find")
    public ResponseEntity<List<ReservationExchangesDto>> getAllRequest(@RequestParam int rno){
        List<ReservationExchangesDto> list = redisService.getAllRequest(rno);
        return ResponseEntity.ok(list);
    }// func end

    /**
     * 교환요청 거절
     *
     * @param from_rno 요청자 예매번호
     * @return boolean 성공 : true , 실패 : false
     */
    @DeleteMapping("")
    public ResponseEntity<?> rejectChange(@RequestParam int from_rno){
        ReservationExchangesDto dto = redisService.getRequest(from_rno);
        boolean result = reservationexchangesService.rejectChange(from_rno);
        if (result){
            if (dto != null){
                String msg = "좌석 교환 요청이 거절되었습니다.";
                reservationexchangesService.responseMessage(dto.getFrom_mno(), msg );
            }// if end
        }// if end
        return ResponseEntity.ok(result);
    }// func end

    /**
     * 교환요청 수락
     *
     * @param from_rno 요청자 예매번호
     * @return boolean 성공 : true , 실패 : false0
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptChange(@RequestParam int from_rno ){
        int mno = membersService.getLoginMember().getMno();
        ReservationExchangesDto dto = redisService.getRequest(from_rno);
        boolean result = reservationexchangesService.acceptChange(mno, from_rno);
        if (result){
            if (dto != null){
                String msg = "좌석 교환 요청이 수락되었습니다.";
                reservationexchangesService.responseMessage(dto.getFrom_mno(), msg );
            }// if end
        }// if end
        return ResponseEntity.ok(result);
    }// func end

}//func end
