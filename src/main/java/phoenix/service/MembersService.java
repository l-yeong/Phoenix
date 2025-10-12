package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;
import org.springframework.stereotype.Service;
import phoenix.security.JwtUtil;
import phoenix.util.PasswordUtil;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 회원 비즈니스 로직: 회원가입/자격검증/이메일 인증 처리.
 * <p><b>토큰 생성/저장 분리.</b></p>
 */
@Service
@RequiredArgsConstructor
public class MembersService {

    private final MembersMapper membersMapper;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final TokenService tokenService; // Redis 기반 TokenService 추가

    // signUp(), emailSend(), verifyEmail() 등 기존 그대로 유지 (토큰 관련 제거)


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
        System.out.println("[LOGIN-DEBUG] member = " + member);

        if (member != null) {
            System.out.println("[LOGIN-DEBUG] email_verified = " + member.getEmail_verified());
            System.out.println("[LOGIN-DEBUG] password matches = " + PasswordUtil.matches(rawPassword, member.getPassword_hash()));
        }

        if( member != null
                && PasswordUtil.matches(rawPassword , member.getPassword_hash())
                && Boolean.TRUE.equals(member.getEmail_verified())){ // 인증된 회원만 로그인 가능

            // JWT 생성
            String accessToken = jwtUtil.generateToken(mid);
            String refreshToken = jwtUtil.generateRefreshToken(mid);

            // Redis에 Refresh Token 저장 (7일 TTL)
            tokenService.saveRefreshToken(mid, refreshToken, Duration.ofDays(7).toMinutes());

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

    /**
     *  이메일 인증 코드 요청 메소드
     * */
    public boolean emailSend(String mid){
        MembersDto member = membersMapper.findByMid(mid);
        if(member == null ) return false;

        emailService.sendAuthCode(member.getEmail());
        return true;
    } // func e


    public boolean emailSendByEmail(String email) {
        MembersDto member = membersMapper.findByEmail(email);
        if (member == null) return false;

        emailService.sendAuthCode(email);
        return true;
    }

}//func end
