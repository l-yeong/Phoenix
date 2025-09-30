package service;

import lombok.RequiredArgsConstructor;
import model.mapper.Reservation_ExchangeMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Reservation_ExchangesService {
    private final Reservation_ExchangeMapper reservation_exchangeMapper;

}//func end
