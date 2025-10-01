package service;

import lombok.RequiredArgsConstructor;
import model.dto.MembersDto;
import model.mapper.MembersMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MembersService {
    private final MembersMapper membersMapper;

    // 회원가입
    public boolean signUp(MembersDto member) {
        if (membersMapper.signUp(member.getMid()) > 0) {
            return false; // 이미 존재하는 아이디
        }
        return membersMapper.signUp(member) > 0;
    }

    // 로그인
    public MembersDto login(String mid, String password_hash) {
        return membersMapper.login(mid, password_hash);
    }

}//func end
