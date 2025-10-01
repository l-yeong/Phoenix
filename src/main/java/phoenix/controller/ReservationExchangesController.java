package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.ReservationExchangesService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class ReservationExchangesController {
    private final ReservationExchangesService reservationexchangesService;

}//func end
