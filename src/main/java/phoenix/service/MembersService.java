package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;
import org.springframework.stereotype.Service;
import phoenix.security.JwtUtil;
import phoenix.util.PasswordUtil;

import java.time.LocalDateTime;

/*
*   회원 비지니스 로직 담당
*   - 회원가입 , 로그인 , 비밀번호 암호화 , JWT 발급
* */
@Service
@RequiredArgsConstructor
public class MembersService {
    private final MembersMapper membersMapper;
    private final JwtUtil jwtUtil;

//    // 회원가입
//    public boolean signUp(MembersDto member) {
//        // 아이디 중복 체크
//        if (membersMapper.checkDuplicateId(member.getMid()) > 0) {
//            return false; // 이미 존재하는 아이디
//        }
//        return membersMapper.signUp(member) > 0;
//    }
//
//    // 로그인
//    public MembersDto login(String mid, String password_hash) {
//        return membersMapper.login(mid, password_hash);
//    }

    /**
     *  회원가입 메소드
     * @param membersDto 회원정보 DTO (mid, password_hash , mname , email)
     * @return boolean ( true : 성공 , false : 실패 )
     */
    public boolean signUp(MembersDto membersDto){

        // 비밀번호 null 방지
        if( membersDto.getPassword_hash() != null || membersDto.getPassword_hash().isEmpty()){
            throw new IllegalArgumentException("비밀번호가 비어있습니다.");
        }

        membersDto.setPassword_hash(PasswordUtil.encode(membersDto.getPassword_hash())); // 비밀번호 암호화
        membersDto.setStatus("active");
        membersDto.setExchange(true);
        membersDto.setEmail_verified(false);
        return membersMapper.signUp(membersDto) > 0;
    } // func e


    /**
     *  로그인 + Access/Refresh Token 발급 메소드
     * @param mid  아이디
     * @param rawPassword 입력 비밀번호 (암호화 전)
     * @return Access Token (JWT 토큰 / 실패시 null )
     * */
    public String login( String mid , String rawPassword ){
        MembersDto member = membersMapper.findByMid(mid);
        if( member != null && PasswordUtil.matches(rawPassword , member.getPassword_hash())){
            String accessToken = jwtUtil.generateToken(mid);
            String refreshToken = jwtUtil.generateRefreshToken(mid);

            // DB에 리프레시 토큰 저장
            member.setRefresh_token(refreshToken);
            member.setRefresh_token_expire(LocalDateTime.now().plusDays(7).toString());
            membersMapper.saveRefreshToken(member);

            return accessToken;
        }
        return null;
    } // func e


    /**
     *  이메일 인증 완료 처리 메소드
     * */
    public boolean verityEmail( String email ){
        return membersMapper.verifyEmail(email) > 0;
    } // func e

}//func end
