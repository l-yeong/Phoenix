package controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.PaymentsService;

@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class PaymentsController {
    private final PaymentsService paymentsService;

}//func end
