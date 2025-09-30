package service;

import lombok.RequiredArgsConstructor;
import model.mapper.PaymentsMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentsService {
    private final PaymentsMapper paymentsMapper;

}//func end
