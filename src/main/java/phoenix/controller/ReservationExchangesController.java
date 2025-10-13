package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.ReservationExchangesDto;
import phoenix.service.RedisService;
import phoenix.service.ReservationExchangesService;

import java.util.List;

@RequestMapping("/exchange")
@RestController
@RequiredArgsConstructor
public class ReservationExchangesController {
    private final ReservationExchangesService reservationexchangesService;
    private final RedisService redisService;

    /**
     * 교환요청 등록
     *
     * @param dto 요청 dto
     * @return boolean 성공 : true , 실패 : false
     */
    @PostMapping("")
    public ResponseEntity<?> saveRequest(ReservationExchangesDto dto){
        boolean result = reservationexchangesService.requestChange(dto);
        return ResponseEntity.ok(result);
    }// func end

    /**
     * 로그인한 회원한테 온 요청목록 조회
     *
     * @param to_rno 로그인한 회원 얘매번호
     * @return List<ReservationExchangesDto> 요청목록
     */
    @GetMapping("/findAll")
    public ResponseEntity<List<ReservationExchangesDto>> getAllRequest(@RequestParam int to_rno){
        List<ReservationExchangesDto> list = redisService.getAllRequest(to_rno);
        return ResponseEntity.ok(list);
    }// func end

}//func end
