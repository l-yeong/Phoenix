package phoenix.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.service.MembersService;
import phoenix.service.RedisService;
import phoenix.service.ReservationExchangesService;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> saveRequest(@RequestBody ReservationExchangesDto dto){
        MembersDto loginMember = membersService.getLoginMember();
        int mno = loginMember.getMno();
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }
        dto.setFrom_mno(mno);
        int result = reservationexchangesService.requestChange(dto);
        System.out.println("result = " + result);
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
        System.out.println("list = " + list);
        return ResponseEntity.ok(list);
    }// func end

    /**
     * 교환요청 거절
     *
     * @param rno 요청자 예매번호
     * @return boolean 성공 : true , 실패 : false
     */
    @DeleteMapping("/reject")
    public ResponseEntity<?> rejectChange(@RequestParam int rno){
        ReservationExchangesDto dto = redisService.getRequest(rno);
        boolean result = reservationexchangesService.rejectChange(rno);
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
     * @param rno 요청자 예매번호
     * @return boolean 성공 : true , 실패 : false0
     */
    @PostMapping("/accept")
    public ResponseEntity<?> acceptChange(@RequestParam int rno ){
        MembersDto loginMember = membersService.getLoginMember();
        int mno = loginMember.getMno();
        System.out.println("mno = " + mno);
        if (loginMember == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인 정보가 없습니다."));
        }
        ReservationExchangesDto dto = redisService.getRequest(rno);
        boolean result = reservationexchangesService.acceptChange(mno, rno);
        System.out.println("result = " + result);
        if (result){
            if (dto != null){
                String msg = "좌석 교환 요청이 수락되었습니다.";
                reservationexchangesService.responseMessage(dto.getFrom_mno(), msg );
            }// if end
        }// if end
        return ResponseEntity.ok(result);
    }// func end

}//func end
