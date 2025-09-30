package service;

import lombok.RequiredArgsConstructor;
import model.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationsService {
    private final ReservationMapper reservationMapper;

}//func end
