package service;

import lombok.RequiredArgsConstructor;
import model.mapper.MembersMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembersService {
    private final MembersMapper membersMapper;

}//func end
