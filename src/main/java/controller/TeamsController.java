package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.TeamsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class TeamsController {
    private final TeamsService teamsService;

}//func end
