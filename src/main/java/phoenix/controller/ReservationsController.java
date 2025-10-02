package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.ReservationsDto;
import phoenix.service.ReservationsService;

import java.util.List;

@RequestMapping("/reserve")
@RestController
@RequiredArgsConstructor
public class ReservationsController {
    private final ReservationsService reservationsService;

    /**
     * 예매내역조회
     *
     * @param session
     * @return List<ReservationsDto>
     */
    @GetMapping("/print")
    public ResponseEntity<List<ReservationsDto>> reservePrint(HttpSession session){
        int mno = (int) session.getAttribute("logMno");
        List<ReservationsDto> list = reservationsService.reservePrint(mno);
        return ResponseEntity.ok().body(list);
    }// func end

    /**
     * 예매내역 상세조회
     *
     * @param rno
     * @param session
     * @return ReservationsDto
     */
    @GetMapping("/info")
    public ResponseEntity<ReservationsDto> reserveInfo( @RequestParam int rno , HttpSession session){
        int mno = (int) session.getAttribute("logMno");
        ReservationsDto dto = reservationsService.reserveInfo(mno,rno);
        return ResponseEntity.ok().body(dto);
    }// func end

    /**
     * 예매내역 수정
     *
     * @param rno
     * @param sno
     * @param session
     * @return boolean
     */
    @PutMapping("/update")
    public ResponseEntity<Boolean> reserveUpdate(@RequestParam int rno , @RequestParam int sno , HttpSession session){
        int mno = (int) session.getAttribute("logMno");
        boolean result = reservationsService.reserveUpdate(rno,sno,mno);
        return ResponseEntity.ok().body(result);
    }// func end
}//class end
