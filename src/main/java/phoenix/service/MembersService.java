package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
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
    private final EmailService emailService;


    /**
     *  회원가입 메소드
     * @param membersDto 회원정보 DTO (mid, password_hash , mname , email)
     * @return boolean ( true : 성공 , false : 실패 )
     */
    @Transactional
    public boolean signUp(MembersDto membersDto){

        // 비밀번호 null 방지 유효성 검사
        if( membersDto.getPassword_hash() == null || membersDto.getPassword_hash().isEmpty()){
            throw new IllegalArgumentException("비밀번호가 비어있습니다.");
        }

        // 비밀번호 해시화
        membersDto.setPassword_hash(PasswordUtil.encode(membersDto.getPassword_hash()));

        // 초기 상태값 설정
        membersDto.setStatus("active");
        membersDto.setExchange(true);
        membersDto.setEmail_verified(false);

        // db 저장
        boolean result = membersMapper.signUp(membersDto) > 0;

        // 이메일 인증코드 발송( Redis + Gmail )
        if(result){
            emailService.sendAuthCode(membersDto.getEmail());
        }

        return result;
    } // func e


    /**
     *  로그인 + Access/Refresh Token 발급 메소드
     * @param mid  아이디
     * @param rawPassword 입력 비밀번호 (암호화 전)
     * @return Access Token (JWT 토큰 / 실패시 null )
     * */
    @Transactional
    public String login( String mid , String rawPassword ){
        MembersDto member = membersMapper.findByMid(mid);

        if( member != null
                && PasswordUtil.matches(rawPassword , member.getPassword_hash())
                && Boolean.TRUE.equals(member.getEmail_verified())){ // 인증된 회원만 로그인 가능

            // JWT 생성
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
    public boolean verityEmail( String email , String code ){

        boolean verified = emailService.verifyCode(email , code );

        if(verified){
            membersMapper.verifyEmail(email);
        }

        return verified;
    } // func e

}//func end
