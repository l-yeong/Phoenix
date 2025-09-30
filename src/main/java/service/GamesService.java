package service;

import lombok.RequiredArgsConstructor;
import model.mapper.GamesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GamesService {
    private final GamesMapper gamesMapper;

}//func end
