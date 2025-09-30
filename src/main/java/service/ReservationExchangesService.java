package service;

import lombok.RequiredArgsConstructor;
import model.mapper.ReservationExchangeMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationExchangesService {
    private final ReservationExchangeMapper reservationexchangeMapper;

}//func end
