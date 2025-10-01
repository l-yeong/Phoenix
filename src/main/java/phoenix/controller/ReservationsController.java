package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.ReservationsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class ReservationsController {
    private final ReservationsService reservationsService;

}//func end
