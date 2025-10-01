package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.AutoAssignLogService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class AutoAssignLogController {
    private final AutoAssignLogService autoAssignLogService;

}//func end
