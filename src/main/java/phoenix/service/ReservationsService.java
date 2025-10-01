package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.ReservationMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationsService {
    private final ReservationMapper reservationMapper;

}//func end
