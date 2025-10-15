package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;
import org.springframework.stereotype.Service;
import phoenix.security.JwtUtil;
import phoenix.util.PasswordUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

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

        if (member.getBirthdate() == null) {
            member.setBirthdate(String.valueOf(LocalDate.of(1900, 1, 1))); // 기본 생일
        }

        if (member.getMphone() == null) {
            member.setMphone("000-0000-0000"); // 임시 전화번호
        }

        if( member != null
                && PasswordUtil.matches(rawPassword , member.getPassword_hash())
                && Boolean.TRUE.equals(member.getEmail_verified())){ // 인증된 회원만 로그인 가능

            // JWT 생성
            String accessToken = jwtUtil.generateToken(member);
            String refreshToken = jwtUtil.generateRefreshToken(member.getMid());

            // Redis에 Refresh Token 저장 (7일 TTL)
            tokenService.saveRefreshToken(member.getMid(), refreshToken, Duration.ofDays(7).toMinutes());

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
    } // func e


    /**
     * 회원 정보 수정
     *
     * @param mid 수정 대상 회원 아이디
     * @param dto 수정할 회원 정보 DTO
     * @return 수정 성공 여부
     */
    public boolean infoUpdate(String mid, MembersDto dto) {
        MembersDto existing = membersMapper.findByMid(mid);
        if (existing == null) return false;

        // 이메일 변경 여부 확인
        if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
            dto.setEmail(existing.getEmail());
        } else if (!existing.getEmail().equals(dto.getEmail())) {
            dto.setEmail_verified(false);
        } else {
            dto.setEmail_verified(existing.getEmail_verified()); // 기존 인증 상태 유지
        }

        // email_verified가 null일 경우 기본값 true로 보정
        if (dto.getEmail_verified() == null) {
            dto.setEmail_verified(existing.getEmail_verified());
        }

        return membersMapper.infoUpdate(mid, dto) > 0;
    } // func e

    /**
     * 로그인 상태에서 비밀번호 변경
     *
     * @param mid 회원 아이디
     * @param currentPwd 현재 비밀번호
     * @param newPwd 새 비밀번호
     * @return 변경 성공 여부
     */
    public boolean pwdUpdate(String mid, String currentPwd, String newPwd) {
        MembersDto member = membersMapper.findByMid(mid);
        if (member == null) return false;

        if (!passwordEncoder.matches(currentPwd, member.getPassword_hash())) return false;

        String newHash = passwordEncoder.encode(newPwd);
        return membersMapper.pwdUpdate(mid, newHash) > 0;
    } // func e

    /**
     * 회원 탈퇴
     *
     * @param mid 회원 아이디
     * @param password 입력한 비밀번호
     * @return 탈퇴 성공 여부
     */
    public boolean memberDelete(String mid, String password) {
        MembersDto member = membersMapper.findByMid(mid);
        if (member == null) return false;

        if (!passwordEncoder.matches(password, member.getPassword_hash())) return false;
        return membersMapper.memberDelete(mid) > 0;
    } // func e


    /**
     * 아이디(mid)로 회원 정보 조회
     * @param mid 회원 아이디
     * @return MembersDto (없으면 null)
     */
    public MembersDto findByMid(String mid) {
        return membersMapper.findByMid(mid);
    } // func e


    /* ==============================
            아이디 찾기
    ============================== */

    /** [1] 이름+전화번호+이메일 확인 후 인증메일 발송 */
    public boolean requestFindId(String mname, String mphone, String email) {
        MembersDto member = membersMapper.findByNamePhoneEmail(mname, mphone, email);
        if (member == null) return false;

        String code = emailService.sendAuthCode(email);
        redisTemplate.opsForValue().set("findid:pending:" + email, code, 5, TimeUnit.MINUTES);
        return true;
    } // func e

    /** [2] 인증코드 검증 */
    public boolean verifyFindIdCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("findid:pending:" + email);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete("findid:pending:" + email);
            redisTemplate.opsForValue().set("findid:verified:" + email, "true", 5, TimeUnit.MINUTES);
            return true;
        }
        return false;
    } // func e

    /** [3] 인증 완료 후 아이디 반환 */
    public String getIdAfterVerification(String email) {
        Boolean verified = redisTemplate.hasKey("findid:verified:" + email);
        if (verified == null || !verified) return null;

        MembersDto member = membersMapper.findByEmail(email);
        return member != null ? member.getMid() : null;
    } // func e

    /* ==============================
            비밀번호 재설정
    ============================== */

    /** [1] mid + 이름 + 이메일 확인 후 인증메일 발송 */
    public boolean requestFindPwd(String mid, String mname, String email) {
        MembersDto member = membersMapper.findByMidNameEmail(mid, mname, email);
        if (member == null) return false;

        String code = emailService.sendAuthCode(email);
        redisTemplate.opsForValue().set("findpwd:pending:" + email, code, 5, TimeUnit.MINUTES);
        return true;
    } // func e

    /** [2] 이메일 인증 코드 검증 */
    public boolean verifyFindPwdCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("findpwd:pending:" + email);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete("findpwd:pending:" + email);
            redisTemplate.opsForValue().set("findpwd:verified:" + email, "true", 5, TimeUnit.MINUTES);
            return true;
        }
        return false;
    } // func e

    /** [3] 인증 완료 후 임시 비밀번호 발급
     * @param email
     * @return*/
    public boolean resetPassword(String email) {
        Boolean verified = redisTemplate.hasKey("findpwd:verified:" + email);
        if (verified == null || !verified) return false;

        MembersDto member = membersMapper.findByEmail(email);
        if (member == null) return false;

        String tempPwd = UUID.randomUUID().toString().substring(0, 8) + "!";
        String hash = passwordEncoder.encode(tempPwd);
        membersMapper.pwdUpdate(member.getMid(), hash);

        emailService.sendSimpleMail(
                email,
                "[Phoenix] 임시 비밀번호 발급 안내",
                "인증이 완료되었습니다.\n\n새로운 임시 비밀번호: " + tempPwd + "\n\n로그인 후 반드시 변경해주세요."
        );

        redisTemplate.delete("findpwd:verified:" + email);
        return true;
    } // func e


}//func end
