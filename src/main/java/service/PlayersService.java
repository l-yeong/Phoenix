package service;

import lombok.RequiredArgsConstructor;
import model.mapper.PlayersMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayersService {
    private final PlayersMapper playersMapper;

}//func end
