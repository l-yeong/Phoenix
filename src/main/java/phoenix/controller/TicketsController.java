package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.TicketsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class TicketsController {
    private final TicketsService ticketsService;

}//func end
