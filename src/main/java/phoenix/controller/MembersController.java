package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
     */
    @PostMapping("signup")
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
     * "mid": "test10",
     * "password_hash": "abcd1234"
     * }
     * */
    @PostMapping("login")
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

} // class e
