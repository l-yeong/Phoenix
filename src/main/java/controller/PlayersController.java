package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.PlayersService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class PlayersController {
    private final PlayersService playersService;

}//func end
