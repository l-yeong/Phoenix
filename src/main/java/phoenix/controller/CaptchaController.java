package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.CaptchaDto;
import phoenix.service.CaptchaService;
import phoenix.service.GateService;
import phoenix.service.MembersService;

@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final GateService gateService;
    private final MembersService membersService;

    /**
     * 캡차 발급
     * GET /captcha/new?gno=123
     * - 게이트 세션이 살아있지 않으면 401
     */
    @GetMapping("/new")
    public ResponseEntity<CaptchaDto.NewCaptchaResponse> create(@RequestParam int gno) throws Exception {
        int mno = membersService.getLoginMember().getMno();
        if (!gateService.isEntered(mno, gno)) {
            // 게이트 만료/미입장
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var pair = captchaService.newCaptcha(); // (token, imageBase64)
        return ResponseEntity.ok(new CaptchaDto.NewCaptchaResponse(pair.token(), pair.imageBase64()));
    }

    /**
     * 캡차 검증
     * POST /captcha/verify?gno=123
     * Body: { "token": "...", "answer": "..." }
     * - 게이트 세션이 살아있지 않으면 401
     * - 바디 누락/필수값 누락이면 400
     */
    @PostMapping("/verify")
    public ResponseEntity<CaptchaDto.VerifyResponse> verify(
            @RequestParam int gno,
            @RequestBody(required = false) CaptchaDto.VerifyRequest req
    ) {
        if (req == null || req.getToken() == null || req.getAnswer() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        int mno = membersService.getLoginMember().getMno();
        if (!gateService.isEntered(mno, gno)) {
            // 게이트 만료/미입장
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int ok = captchaService.verify(req.getToken(), req.getAnswer()); // 1/0/-1
        return ResponseEntity.ok(new CaptchaDto.VerifyResponse(ok));
    }
}