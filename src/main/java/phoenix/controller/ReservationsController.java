package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.ReservationsDto;
import phoenix.service.ReservationsService;

import java.util.List;
import java.util.Map;

@RequestMapping("/reserve")
@RestController
@RequiredArgsConstructor
public class ReservationsController {
    private final ReservationsService reservationsService;

    /**
     * 예매내역조회
     *
     * @param session
     * @return List<Map<String,Object>>
     */
    @GetMapping("/print")
    public ResponseEntity<?> reservePrint(HttpSession session){
        int mno = (int) session.getAttribute("logMno");
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
     * @param session
     * @return boolean
     */
    @PutMapping("/update")
    public ResponseEntity<Boolean> reserveUpdate(@RequestBody ReservationsDto dto , HttpSession session){
        int mno = (int) session.getAttribute("logMno");
        boolean result = reservationsService.reserveUpdate(dto.getSno(),dto.getRno(),mno);
        return ResponseEntity.ok().body(result);
    }// func end

    /**
     * 예매취소
     *
     * @param rno
     * @param session
     * @return boolean
     */
    @PutMapping("/cancle")
    public ResponseEntity<Boolean> reserveCancle(@RequestParam int rno , HttpSession session){
        int mno = (int) session.getAttribute("logMno");
        boolean result = reservationsService.reserveCancle(rno , mno);
        return ResponseEntity.ok().body(result);
    }// func end
}//class end
