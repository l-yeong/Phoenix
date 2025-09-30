package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.MembersService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class MembersController {
    private final MembersService membersService;

}//func end
