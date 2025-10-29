package phoenix.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;
import org.springframework.stereotype.Service;
import phoenix.security.JwtUtil;
import phoenix.util.PasswordUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
     * 회원가입 메소드
     *
     * @param membersDto 회원정보 DTO (mid, password_hash , mname , email)
     * @return boolean ( true : 성공 , false : 실패 )
     */
    @Transactional
    public boolean signUp(MembersDto membersDto) {

        // 소셜회원이면 비밀번호 검사 스킵
        if (membersDto.getProvider() == null || membersDto.getProvider().isBlank()) {
            if (membersDto.getPassword_hash() == null || membersDto.getPassword_hash().isEmpty()) {
                throw new IllegalArgumentException("비밀번호가 비어있습니다.");
            }
            membersDto.setPassword_hash(passwordEncoder.encode(membersDto.getPassword_hash()));
        }

        // 초기 상태값 설정
        membersDto.setStatus("active");
        membersDto.setExchange(true);
        membersDto.setEmail_verified(true);

        // db 저장
        boolean result = membersMapper.signUp(membersDto) > 0;

        return result;
    } // func e


    /**
     * 로그인 + Access/Refresh Token 발급 메소드
     *
     * @param mid         아이디
     * @param rawPassword 입력 비밀번호 (암호화 전)
     * @return Access Token (JWT 토큰 / 실패시 null )
     */
    @Transactional
    public String login(String mid, String rawPassword,String fcmToken) { //String fcmToken 추가(firebase)알림
        MembersDto member = membersMapper.findByMid(mid);
        System.out.println("[LOGIN-DEBUG] member = " + member);

        if (member != null) {
            System.out.println("[LOGIN-DEBUG] email_verified = " + member.getEmail_verified());
            System.out.println("[LOGIN-DEBUG] password matches = " + PasswordUtil.matches(rawPassword, member.getPassword_hash()));
        }

        if ("withdrawn".equalsIgnoreCase(member.getStatus())) {
            throw new IllegalStateException("withdrawn"); // 로그인 차단 + 안내페이지로 이동용
        }

        if (member.getBirthdate() == null) {
            member.setBirthdate(String.valueOf(LocalDate.of(1900, 1, 1))); // 기본 생일
        }

        if (member.getMphone() == null) {
            member.setMphone("000-0000-0000"); // 임시 전화번호
        }

        if (member != null
                && PasswordUtil.matches(rawPassword, member.getPassword_hash())
                && Boolean.TRUE.equals(member.getEmail_verified())) { // 인증된 회원만 로그인 가능

            if(fcmToken!=null && !fcmToken.isEmpty()){
                try{
                    membersMapper.ticketTokenWrite(mid,fcmToken);
                    System.out.println("[LOGIN] FCM 토큰 저장 완료 mid= "+mid);
                } catch (Exception e) {
                    System.out.println("[LOGIN] FCM 토큰 저장 실패 mid= "+mid +"오류: "+e.getMessage());
                }//catch end
            }//fcmToken if end

            // JWT 생성
            String accessToken = jwtUtil.generateToken(member);
            String refreshToken = jwtUtil.generateRefreshToken(member.getMid());

            // Redis에 Refresh Token 저장 (7일 TTL)
            tokenService.saveRefreshToken(member.getMid(), refreshToken, Duration.ofDays(7).toMinutes());

            // 세션 기반 인증 정보 수동 저장
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    member, // Principal
                    null, // Credentials
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return accessToken;
        }
        return null;
    } // func e


    /**
     * 이메일 인증 코드 요청 메소드
     */
    public boolean emailSendByEmail(String email) {
        MembersDto member = membersMapper.findByEmail(email);
        if (member != null) return false;

        emailService.sendAuthCode(email);
        return true;
    } // func e

    /**
     * 이메일 인증 완료 처리 메소드
     */
    public boolean verityEmail(String email, String code) {

        boolean verified = emailService.verifyCode(email, code);

        if (verified) {
            membersMapper.verifyEmail(email);
        }

        return verified;
    } // func e


    /**
     * 회원 정보 수정
     *
     * @param mid 수정 대상 회원 아이디
     * @param dto 수정할 회원 정보 DTO
     * @return 수정 성공 여부
     */
    @Transactional
    public boolean infoUpdate(String mid, MembersDto dto) {
        MembersDto existing = membersMapper.findByMid(mid);
        if (existing == null) return false;

        // 소셜회원일 경우 이메일 변경 금지
        if (existing.getProvider() != null && !existing.getProvider().isBlank()) {
            dto.setEmail(existing.getEmail()); // 원래 이메일 유지
            dto.setEmail_verified(existing.getEmail_verified()); // 이메일 인증상태도 유지
        } else {

            // 이메일 변경 여부 확인
            if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
                dto.setEmail(existing.getEmail());
            } else if (!existing.getEmail().equals(dto.getEmail())) {
                dto.setEmail_verified(false); // 이메일 변경시 인증 해제
            } else {
                dto.setEmail_verified(existing.getEmail_verified()); // 기존 인증 상태 유지
            }

            // email_verified가 null일 경우 기본값 true로 보정
            if (dto.getEmail_verified() == null) {
                dto.setEmail_verified(existing.getEmail_verified());
            }
        }

        // mid는 인증회원 기준으로 유지
        dto.setMid(mid);

        return membersMapper.infoUpdate(mid, dto) > 0;

    } // func e

    /**
     * 로그인 상태에서 비밀번호 변경
     *
     * @param mid        회원 아이디
     * @param currentPwd 현재 비밀번호
     * @param newPwd     새 비밀번호
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
     * 회원 탈퇴 (상태만 변경)
     *
     * @param mid      회원 아이디
     * @param password 입력한 비밀번호
     * @return 탈퇴 성공 여부
     */
    public boolean memberDelete(String mid, String password) {
        MembersDto member = membersMapper.findByMid(mid);
        if (member == null) return false;


        // 소셜 회원은 비밀번호 검증 스킵
        if (member.getProvider() != null && !member.getProvider().isBlank()) {
            return membersMapper.updateStatus(mid, "withdrawn") > 0;
        }

        // 일반 회원은 비밀번호 검증 필수
        if (!passwordEncoder.matches(password, member.getPassword_hash())) {
            return false;
        }

        return membersMapper.updateStatus(mid, "withdrawn") > 0;

    } // func e

    /**
     * 휴면 계정 복구 (테스트는 패널티 1분 / 나중에 1일 or 7일로 설정 )
     *
     * @param mid
     * @return
     */
    public boolean changeStatus(String mid) {
        MembersDto member = membersMapper.findByMid(mid);
        if (member == null) return false;

        // 1분 패널티 적용
        if (member.getLast_status_change() != null) {
            long minute = ChronoUnit.MINUTES.between(
                    member.getLast_status_change(),
                    LocalDateTime.now()
            );
            if (minute < 1) {
                throw new IllegalStateException("1분 이내에는 상태를 변경할 수 없습니다.");
            }
        }

        return membersMapper.changeStatus(mid) > 0;

    } // func e


    /**
     * 아이디(mid)로 회원 정보 조회
     *
     * @param mid 회원 아이디
     * @return MembersDto (없으면 null)
     */
    public MembersDto findByMid(String mid) {
        return membersMapper.findByMid(mid);
    } // func e


    /* ==============================
            아이디 찾기
    ============================== */

    /**
     * [1] 이름+전화번호+이메일 확인 후 인증메일 발송
     */
    public boolean requestFindId(String mname, String mphone, String email) {
        MembersDto member = membersMapper.findByNamePhoneEmail(mname, mphone, email);
        if (member == null) return false;

        String code = emailService.sendAuthCode(email);
        redisTemplate.opsForValue().set("findid:pending:" + email, code, 5, TimeUnit.MINUTES);
        return true;
    } // func e

    /**
     * [2] 인증코드 검증
     */
    public boolean verifyFindIdCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("findid:pending:" + email);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete("findid:pending:" + email);
            redisTemplate.opsForValue().set("findid:verified:" + email, "true", 5, TimeUnit.MINUTES);
            return true;
        }
        return false;
    } // func e

    /**
     * [3] 인증 완료 후 아이디 반환
     */
    public String getIdAfterVerification(String email) {
        Boolean verified = redisTemplate.hasKey("findid:verified:" + email);
        if (verified == null || !verified) return null;

        MembersDto member = membersMapper.findByEmail(email);
        return member != null ? member.getMid() : null;
    } // func e

    /* ==============================
            비밀번호 재설정
    ============================== */

    /**
     * [1] mid + 이름 + 이메일 확인 후 인증메일 발송
     */
    public boolean requestFindPwd(String mid, String mname, String email) {
        MembersDto member = membersMapper.findByMidNameEmail(mid, mname, email);
        if (member == null) return false;

        String code = emailService.sendAuthCode(email);
        redisTemplate.opsForValue().set("findpwd:pending:" + email, code, 5, TimeUnit.MINUTES);
        return true;
    } // func e

    /**
     * [2] 이메일 인증 코드 검증
     */
    public boolean verifyFindPwdCode(String email, String code) {
        String savedCode = redisTemplate.opsForValue().get("findpwd:pending:" + email);
        if (savedCode != null && savedCode.equals(code)) {
            redisTemplate.delete("findpwd:pending:" + email);
            redisTemplate.opsForValue().set("findpwd:verified:" + email, "true", 5, TimeUnit.MINUTES);
            return true;
        }
        return false;
    } // func e

    /**
     * [3] 인증 완료 후 임시 비밀번호 발급
     *
     * @param email
     * @return
     */
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

    /**
     * 현재 로그인한 회원 정보 반환(세션 기반)
     * - 일반 로그인(UserDetails)
     * - 소셜 로그인(DefaultOAuth2User)
     * - 직접 저장한 MembersDto
     */
    public MembersDto getLoginMember() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        Object principal = auth.getPrincipal();

        // 비로그인 상태
        if (principal.equals("anonymousUser")) return null;

        // 소셜 로그인 시 principal에 MembersDto로 저장한 경우
        if (principal instanceof MembersDto memberDto) {
            return memberDto;
        }

        // 일반 로그인
        if (principal instanceof UserDetails userDetails) {
            String mid = userDetails.getUsername();
            return membersMapper.findByMid(mid);
        }

        // OAuth2 로그인(DefaultOAuth2User)
        if (principal instanceof DefaultOAuth2User oAuth2User) {
            String provider = (String) oAuth2User.getAttribute("provider");

            if (provider == null) {
                Object registration = oAuth2User.getAttributes().get("iss");
                if (registration != null && registration.toString().contains("google")) provider = "google";
                else if (oAuth2User.getAttributes().containsKey("login")) provider = "github";
                else provider = "facebook";
            }

            String providerId = null;
            if (oAuth2User.getAttributes().get("sub") != null)
                providerId = oAuth2User.getAttributes().get("sub").toString();
            else if (oAuth2User.getAttributes().get("id") != null)
                providerId = oAuth2User.getAttributes().get("id").toString();

            return membersMapper.findByProvider(provider, providerId);
        }

        // principal이 String(mid)인 경우 (혹시 남아있을 때 대비)
        if (principal instanceof String mid) {
            return membersMapper.findByMid(mid);
        }

        return null;

    } // func e

    /*===================FireBase Ticket_Token=============================*/

    /**
     * 회원의 FCM 토큰을 DB에 저장 또는 갱신합니다.
     *
     * @param mid   로그인 ID (회원 식별용)
     * @param token Firebase에서 발급받은 FCM registration token
     *              <p>
     *              - 클라이언트(웹/앱)가 로그인 성공 후, FCM 토큰을 발급받으면
     *              해당 토큰과 로그인 ID(mid)를 서버에 전달하여 DB에 저장
     *              <p>
     *              - members 테이블의 ticket_Token 컬럼을 UPDATE 함
     */
    public boolean ticketTokenWrite(String mid, String token) {
        int result = membersMapper.ticketTokenWrite(mid, token);
        return result > 0;
    }//func end

    /**
     * 회원의 FCM 토큰을 조회합니다.
     *
     * @param mid 로그인 ID
     * @return 해당 회원의 FCM registration token (없을 경우 null)
     * <p>
     * 사용 시점:
     * - 푸시 알림을 발송할 때, 특정 회원의 토큰을 조회하여 사용
     */
    public String ticketTokenPrint(String mid) {
        String result = membersMapper.ticketTokenPrint(mid);
        return result;
    }//func end

    /**
     * 특정 회원(mid)의 FCM 토큰으로 푸시 메시지를 발송합니다.
     *
     * @param mid   로그인 ID
     * @param title 알림 제목
     * @param body  알림 내용
     * @return Firebase가 반환한 messageId (성공 시 고유 ID)
     * <p>
     * 회원의 ticket_Token을 DB에서 조회
     * FirebaseMessaging 인스턴스를 통해 알림 전송
     * 실패 시 예외(FirebaseMessagingException) 발생
     */
    public String ticketMessaging(String mid, String title, String body) {
        //회원 토큰 조회
        //members 테이블의 ticket_token 컬럼에서 해당 mid의 FCM TOKEN을 호춣
        String token = membersMapper.ticketTokenPrint(mid);

        //토큰 유효성 검사
        // 토큰이 없거나 빈값이면 예외 발생
        if (token == null || token.isEmpty()) {
            System.out.println("등록된 FCM 토큰이 없습니다. mid =" + mid);
            return null;
        }//if end
        try {
            // 메시지 알림 객체 생성
            Message msg = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder() //푸시 알림의 제목과 본문을 포함하며 사용자의 화면 상단에 알림 표시
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            // FCM 서버로 푸시 전송
            String messageId = FirebaseMessaging.getInstance().send(msg);
            System.out.println("FCM 푸시 전송성공 : mid +" + mid + "Message : " + messageId);
            return messageId;
        } catch (Exception e) {
            System.out.println("FCM 푸시 전송실패 : mid +" + mid + "원인:" + e.getMessage());
        }//catch end
        return null;
    }//func end

}//class end
