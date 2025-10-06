package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.ReservationExchangeMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangesMapper reservationexchangeMapper;
}//func end
