package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.Reservation_ExchangesService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class Reservation_ExchangesController {
    private final Reservation_ExchangesService reservation_exchangesService;

}//func end
