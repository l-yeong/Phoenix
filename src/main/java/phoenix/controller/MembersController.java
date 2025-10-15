package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;
import phoenix.util.ApiResponseUtil;

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
    public ResponseEntity<ApiResponseUtil<?>> login(@RequestBody Map< String , String > request){
        String mid = request.get("mid");
        String password = request.get("password_hash");
        String token = membersService.login(mid , password );

        if( token != null ){
            return ResponseEntity
                    .ok() // 200 OK
                    .body(new ApiResponseUtil<>(true , "로그인 성공" , token));
        } else {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                    .body(new ApiResponseUtil<>(false, "로그인 실패", null));
        } // if e
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
     * @param user 로그인한 회원 정보 (SecurityContext에서 주입)
     * @param dto 수정할 회원 정보 DTO
     * @return 수정 성공 여부 메시지
     */
    @PutMapping("/infoupdate")
    public ResponseEntity<ApiResponseUtil<?>> infoUpdate(@AuthenticationPrincipal MembersDto user,
                                                         @RequestBody MembersDto dto) {
        boolean result = membersService.infoUpdate(user.getMid(), dto);

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
     * @param user 로그인한 회원 정보
     * @param req 비밀번호 변경 요청 JSON
     * @return 변경 성공 여부 메시지
     */
    @PutMapping("/pwdupdate")
    public ResponseEntity<ApiResponseUtil<?>> pwdUpdate(@AuthenticationPrincipal MembersDto user,
                                                        @RequestBody Map<String, String> req) {
        boolean result = membersService.pwdUpdate(
                user.getMid(),
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
     * @param user 로그인한 회원 정보
     * @param req 비밀번호 입력 JSON
     * @return 탈퇴 성공 여부 메시지
     */
    @PostMapping("/delete")
    public ResponseEntity<ApiResponseUtil<?>> memberDelete(@AuthenticationPrincipal MembersDto user,
                                                           @RequestBody Map<String, String> req) {
        boolean result = membersService.memberDelete(user.getMid(), req.get("password_hash"));

        return result
                ? ResponseEntity.ok(new ApiResponseUtil<>(true, "회원 탈퇴가 완료되었습니다.", null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "비밀번호가 일치하지 않습니다.", null));
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
