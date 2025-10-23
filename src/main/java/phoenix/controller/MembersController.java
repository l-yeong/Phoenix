package phoenix.controller;

import com.beust.ah.A;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.security.JwtUtil;
import phoenix.service.MembersService;
import phoenix.util.ApiResponseUtil;
import phoenix.util.PasswordUtil;

import java.util.List;
import java.util.Map;

/**
 *  회원 관련 요청 처리하는 컨트롤러
 *  - 회원가입 / 로그인 API 제공
 * */
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MembersController {

    private final MembersService membersService;
    private final JwtUtil jwtUtil;


    /**
     *  회원가입 메소드
     * @param membersDto 회원가입 정보 JSON
     * @return ResponseEntity<ApiResponse> 응답 상태 + body
     *  탈랜드 테스트용
     * {
     *   "mid": "test10",
     *   "password_hash": "abcd1234",
     *   "mname": "홍길동",
     *   "mphone": "01022223333",
     *   "birthdate": "1995-10-05",
     *   "email": "hong@test.com",
     *   "provider": null,
     *   "provider_id": null,
     *   "pno": null,
     *   "status": "active",
     *   "exchange": true,
     *   "email_verified": false
     * }
     * 회원가입 시 받아야하는 값 많아서 실제로 해야함
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseUtil<?>> signUp(@RequestBody MembersDto membersDto){
        boolean result = membersService.signUp(membersDto);

        if(result){
            return ResponseEntity
                    .status(HttpStatus.CREATED) // 201 Created
                    .body(new ApiResponseUtil<>(true , "회원가입 성공" , null));
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST) // 400 Bad Request
                    .body(new ApiResponseUtil<>(false , "회원가입 실패" , null));
        } // if e
    } // func e

    /**
     *  로그인( JWT 발급) 메소드
     *  탈랜드 테스트용
     *  {
     * "mid": "test1234",
     * "password_hash": "1234"
     * }
     * */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseUtil<?>> login(@RequestBody Map<String, String> request,
                                                    HttpServletRequest httpRequest) {
        String mid = request.get("mid");
        String password = request.get("password_hash");

        // 1. 기존 membersService.login() 은 JWT 발급 로직이지만, 비밀번호 검증은 그대로 사용
        MembersDto member = membersService.findByMid(mid);
        if (member == null || !PasswordUtil.matches(password, member.getPassword_hash())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false, "아이디 또는 비밀번호가 일치하지 않습니다.", null));
        }

        // 상태값 검사 추가(정상(active)/휴면(dormant)/탈퇴(withdrawn) 계정 로그인 불가)
        switch (member.getStatus()){
                case "withdrawn" -> {
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(new ApiResponseUtil<>(false , "탈퇴한 계정입니다. 다시 가입해주세요." , null));
                }
                case "dormant" -> {
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(new ApiResponseUtil<>(false , "휴면 상태의 계정입니다. 관리자에게 문의하세요.",null));
                }
                case "active" -> {

                }
                default -> {
                    return ResponseEntity
                            .status(HttpStatus.FORBIDDEN)
                            .body(new ApiResponseUtil<>(false , "비정상적인 계정 상태입니다." , null));
                }

        } // switch e

        // 2. Authentication 객체 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(member, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        // 3. SecurityContext 에 등록
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        // 4. 세션에 인증정보 저장 (Spring Security가 세션기반 인증 유지 가능하게)
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        // 5. JWT 발급은 유지 — Redis 통계나 추후 로그아웃용으로만
        String token = jwtUtil.generateToken(member);

        Map<String, Object> data = Map.of(
                    "member" , member
                // 필요 시 accessToken도 함께 내려보낼 수 있음 (디버깅용 or 참고용)
        );

        return ResponseEntity
                .ok(new ApiResponseUtil<>(true, "로그인 성공 (세션 저장 완료)", member));

    } // func e

    /**
     * 현재 로그인한 회원 정보 반환 (세션 기반)
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponseUtil<?>> getLoginMemberInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false, "로그인 상태가 아닙니다.", null));
        }

        Object principal = auth.getPrincipal();
        MembersDto member;

        if (principal instanceof MembersDto dto) {
            // 로그인 시 MembersDto를 세션에 저장했을 경우
            member = dto;
        } else {
            // 안전장치 — 혹시 문자열만 저장된 경우 DB 재조회
            String mid = auth.getName();
            member = membersService.findByMid(mid);
        }

        return ResponseEntity.ok(new ApiResponseUtil<>(true, "로그인 회원 정보 반환 성공", member));

    } // func e

    /**
     * 이메일 인증 API (코드 검증)
     * @param request { "email": "user@test.com", "code": "123456" }
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseUtil<?>> verifyEmail(@RequestBody Map<String , String> request ){
        String email = request.get("email");
        String code = request.get("code");

        boolean verified = membersService.verityEmail(email , code);

        return verified
                ? ResponseEntity.ok(new ApiResponseUtil<>(true , "이메일 인증 완료" , null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "인증 코드가 올바르지 않거나 만료되었습니다." , null));
    } // func e

    @PostMapping("/email/send")
    public boolean sendEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return membersService.emailSendByEmail(email);
    } // func e


    /**
     * 회원 정보 수정
     * <p>PUT /members/infoupdate</p>
     * <pre>
     * 요청 JSON 예시:
     * {
     *   "mname": "홍길순",
     *   "mphone": "010-9999-8888",
     *   "email": "user1@newmail.com",
     *   "pno": 12,
     *   "exchange": false
     * }
     * </pre>
     * 이제 이거 안씀 , 맨 아래 getLoginMember()에서 분기 처리 한 dto 쓰면 됨 user 로그인한 회원 정보 (SecurityContext에서 주입)
     * @param dto 수정할 회원 정보 DTO
     * @return 수정 성공 여부 메시지
     */
    @PutMapping("/infoupdate")
    public ResponseEntity<ApiResponseUtil<?>> infoUpdate( @RequestBody MembersDto dto) {

        MembersDto loginMember = membersService.getLoginMember();

        if(loginMember == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false , "로그인이 필요합니다." , null));
        }

        boolean result = membersService.infoUpdate(loginMember.getMid(), dto);

        return result
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "회원 정보가 성공적으로 수정되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "회원 정보 수정 실패", null));
    } // func e


    /**
     * 비밀번호 변경
     * <p>PUT /members/pwdupdate</p>
     * <pre>
     * 요청 JSON 예시:
     * {
     *   "current_password": "1234",
     *   "new_password": "New#5678"
     * }
     * </pre>
     * // 여기도 이제 안씀 @param user 로그인한 회원 정보
     * @param req 비밀번호 변경 요청 JSON
     * @return 변경 성공 여부 메시지
     */
    @PutMapping("/pwdupdate")
    public ResponseEntity<ApiResponseUtil<?>> pwdUpdate(@RequestBody Map<String, String> req) {

        MembersDto loginMember = membersService.getLoginMember();

        if(loginMember == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false , "로그인이 필요합니다." , null));
        }

        boolean result = membersService.pwdUpdate(
                loginMember.getMid(),
                req.get("current_password"),
                req.get("new_password")
        );

        return result
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "비밀번호가 성공적으로 변경되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "현재 비밀번호가 일치하지 않습니다.", null));
    } // func e

    /**
     * 회원 탈퇴
     * <p>POST /members/delete</p>
     * <pre>
     * 요청 JSON 예시:
     * {
     *   "password_hash": "1234"
     * }
     * </pre>
     * // 여기도 안씀 이제 @param user 로그인한 회원 정보
     * @param req 비밀번호 입력 JSON
     * @return 탈퇴 성공 여부 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponseUtil<?>> memberDelete(@RequestBody Map<String, String> req ,
                                                           HttpServletRequest request ) {

        MembersDto loginMember = membersService.getLoginMember();

        if(loginMember == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false , "로그인이 필요합니다." , null));
        }

        boolean result = membersService.memberDelete(loginMember.getMid(), req.get("password_hash"));

        if( result ){
            // 세션 기반 인증 완전 종료
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();

            return ResponseEntity.ok(new ApiResponseUtil<>( true , "회원 탈퇴가 완료되었습니다." , null));

        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false , "비밀번호가 일치하지 않습니다." , null));

    } // func e


    /* ============================
             아이디 찾기
    ============================ */

    /** 아이디 찾기 1단계: 본인정보 확인 후 인증메일 발송 */
    @PostMapping("/findid/request")
    public ResponseEntity<ApiResponseUtil<?>> requestFindId(@RequestBody Map<String, String> req) {
        boolean sent = membersService.requestFindId(req.get("mname"), req.get("mphone"), req.get("email"));
        return sent
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "인증 메일이 발송되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "일치하는 회원 정보가 없습니다.", null));
    } // func e

    /** 아이디 찾기 2단계: 인증 코드 검증 */
    @PostMapping("/findid/verify")
    public ResponseEntity<ApiResponseUtil<?>> verifyFindId(@RequestBody Map<String, String> req) {
        boolean verified = membersService.verifyFindIdCode(req.get("email"), req.get("code"));
        return verified
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "인증이 완료되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "인증 코드가 올바르지 않거나 만료되었습니다.", null));
    } // func e

    /** 아이디 찾기 3단계: 아이디 반환 */
    @GetMapping("/findid")
    public ResponseEntity<ApiResponseUtil<?>> getId(@RequestParam String email) {
        String mid = membersService.getIdAfterVerification(email);
        return (mid != null)
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "아이디 찾기 성공", mid))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "이메일 인증이 완료되지 않았거나 회원이 존재하지 않습니다.", null));
    } // func e

    /* ============================
             비밀번호 찾기
    ============================ */

    /** 비밀번호 찾기 1단계: 본인정보 확인 후 인증메일 발송 */
    @PostMapping("/findpwd/request")
    public ResponseEntity<ApiResponseUtil<?>> requestFindPwd(@RequestBody Map<String, String> req) {
        boolean sent = membersService.requestFindPwd(req.get("mid"), req.get("mname"), req.get("email"));
        return sent
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "비밀번호 재설정 인증 메일이 발송되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "일치하는 회원 정보가 없습니다.", null));
    } // func e

    /** 비밀번호 찾기 2단계: 인증 코드 검증 */
    @PostMapping("/findpwd/verify")
    public ResponseEntity<ApiResponseUtil<?>> verifyFindPwd(@RequestBody Map<String, String> req) {
        boolean verified = membersService.verifyFindPwdCode(req.get("email"), req.get("code"));
        return verified
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "인증이 완료되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "인증 코드가 올바르지 않거나 만료되었습니다.", null));
    } // func e

    /** 비밀번호 찾기 3단계: 임시 비밀번호 발급 */
    @PostMapping("/findpwd/reset")
    public ResponseEntity<ApiResponseUtil<?>> resetPassword(@RequestBody Map<String, String> req) {
        boolean success = membersService.resetPassword(req.get("email"));
        return success
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "임시 비밀번호가 이메일로 발송되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "비밀번호 재설정 실패 (인증 필요 또는 회원 없음).", null));
    } // func e


} // class e
