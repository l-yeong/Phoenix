package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import phoenix.model.dto.TicketsDto;
import phoenix.service.TicketsService;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequestMapping("/ticket")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;

    @GetMapping("/print")
    public ResponseEntity<List<String>> ticketPrint(@RequestParam int mno){
        List<byte[]> qrList = ticketsService.ticketPrint(mno);

        // byte[] → Base64 문자열로 변환
        List<String> base64List = qrList.stream()
                .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                .toList();

        return ResponseEntity.ok(base64List);

    }//func end

}//func end
