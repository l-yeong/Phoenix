package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MembersController {

    private final MembersService membersService;

    // 테스트용 회원가입
    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody MembersDto member) {
        boolean result = membersService.signUp(member);
        return result ? ResponseEntity.ok("회원가입 성공")
                : ResponseEntity.badRequest().body("이미 존재하는 아이디입니다.");
    }
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

    // 테스트용 로그인
    @PostMapping("/login")
    public String login(@RequestBody MembersDto member) {
        MembersDto loginUser = membersService.login(member.getMid(), member.getPassword_hash());
        return (loginUser != null) ?
                "로그인 성공: " + loginUser.getMname() :
                "로그인 실패";
    }
    /*
    탈랜드 body 테스트 폼
     {
      "mid": "user01",
      "password_hash": "1234"
    }
    */

}
