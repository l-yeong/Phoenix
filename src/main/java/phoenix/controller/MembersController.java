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

//    // 테스트용 회원가입
//    @PostMapping("/signup")
//    public ResponseEntity<String> signUp(@RequestBody MembersDto member) {
//        boolean result = membersService.signUp(member);
//        return result ? ResponseEntity.ok("회원가입 성공")
//                : ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
//    }
    /*
    탈랜드 body 테스트 폼
        {
      "mid": "user01",
      "password_hash": "1234",
      "mname": "홍길동",
      "mphone": "010-1234-5678",
      "birthdate": "1970-01-01",
      "email": "user01@example.com"
        }
    */

//    // 테스트용 로그인
//    @PostMapping("/login")
//    public String login(@RequestBody MembersDto member) {
//        MembersDto loginUser = membersService.login(member.getMid(), member.getPassword_hash());
//        return (loginUser != null) ?
//                "로그인 성공: " + loginUser.getMname() :
//                "로그인 실패";
//    }
    /*
    탈랜드 body 테스트 폼
     {
      "mid": "user01",
      "password_hash": "1234"
    }
    */

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

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponseUtil<?>> verifyEmail(@RequestParam Map<String , String> request ){
        String email = request.get("email");
        boolean verified = membersService.verityEmail(email);
        return verified
                ? ResponseEntity.ok(new ApiResponseUtil<>(true , "이메일 인증 완료" , null))
                : ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponseUtil<>(false, "이메일 인증 실패" , null));
    } // func e

} // class e
