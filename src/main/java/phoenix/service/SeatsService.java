package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.dto.SeatDto;
import phoenix.model.dto.SeatsDto;
import phoenix.model.mapper.SeatsMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatsService {
    private final SeatsMapper seatsMapper;

    // 전체좌석 조회
    public List<SeatDto> seatPrint(){
        return seatsMapper.seatPrint();
    }// func end
}//func end
