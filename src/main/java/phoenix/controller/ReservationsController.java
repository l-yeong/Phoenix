package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.ReservationsDto;
import phoenix.service.MembersService;
import phoenix.service.ReservationsService;

import java.util.List;
import java.util.Map;

@RequestMapping("/reserve")
@RestController
@RequiredArgsConstructor
public class ReservationsController {
    private final ReservationsService reservationsService;
    private final MembersService membersService;

    /**
     * 예매내역조회
     *
     * @return List<Map<String,Object>>
     */
    @GetMapping("/print")
    public ResponseEntity<?> reservePrint(){
        int mno = membersService.getLoginMember().getMno();
        List<Map<String,Object>> list = reservationsService.reservePrint(mno);
        return ResponseEntity.ok().body(list);
    }// func end

    /**
     * 예매내역 상세조회
     *
     * @param rno
     * @return  Map<String ,Object>
     */
    @GetMapping("/info")
    public ResponseEntity<?> reserveInfo( @RequestParam int rno ){
        Map<String ,Object> map = reservationsService.reserveInfo(rno);
        return ResponseEntity.ok().body(map);
    }// func end

    /**
     * 예매내역 수정
     *
     * @param dto
     * @return boolean
     */
    @PutMapping("/update")
    public ResponseEntity<Boolean> reserveUpdate(@RequestBody ReservationsDto dto ){
        int mno = membersService.getLoginMember().getMno();
        boolean result = reservationsService.reserveUpdate(dto.getSno(),dto.getRno(),mno);
        return ResponseEntity.ok().body(result);
    }// func end

    /**
     * 예매취소
     *
     * @param rno
     * @return boolean
     */
    @PutMapping("/cancle")
    public ResponseEntity<Boolean> reserveCancle(@RequestParam int rno ){
        int mno = membersService.getLoginMember().getMno();
        boolean result = reservationsService.reserveCancle(rno , mno);
        return ResponseEntity.ok().body(result);
    }// func end

    /**
     * 교환신청 가능한 좌석목록 예매정보
     *
     * @param rno
     * @return List<ReservationsDto>
     */
    @GetMapping("/possible")
    public ResponseEntity<?> seatPossible(@RequestParam int rno){
        List<ReservationsDto> list = reservationsService.seatPossible(rno);
        return ResponseEntity.ok(list);
    }// func end
}//class end
