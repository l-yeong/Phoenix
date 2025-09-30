package service;

import lombok.RequiredArgsConstructor;
import model.mapper.TeamsMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeamsService {
    private final TeamsMapper teamsMapper;

}//func end
