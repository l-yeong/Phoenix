package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.ZonesService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class ZonesController {
    private final ZonesService zonesService;

}//func end
