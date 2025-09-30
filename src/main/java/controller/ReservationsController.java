package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.ReservationsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class ReservationsController {
    private final ReservationsService reservationsService;

}//func end
