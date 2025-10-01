package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.SeatsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class SeatsController {
    private final SeatsService seatsService;

}//func end
