package phoenix.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
}//func end
