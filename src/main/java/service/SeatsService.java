package service;

import lombok.RequiredArgsConstructor;
import model.mapper.SeatsMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SeatsService {
    private final SeatsMapper seatsMapper;

}//func end
