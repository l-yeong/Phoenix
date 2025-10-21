package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.CaptchaDto;
import phoenix.service.CaptchaService;

@RestController
@RequestMapping("/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/new")
    public ResponseEntity<CaptchaDto.NewCaptchaResponse> create() throws Exception {
        var pair = captchaService.newCaptcha();
        return ResponseEntity.ok(new CaptchaDto.NewCaptchaResponse(pair.token(), pair.imageBase64()));
    }

    @PostMapping("/verify")
    public ResponseEntity<CaptchaDto.VerifyResponse> verify(@RequestBody CaptchaDto.VerifyRequest req) {
        boolean ok = captchaService.verify(req.getToken(), req.getAnswer());
        return ResponseEntity.ok(new CaptchaDto.VerifyResponse(ok));
    }

}
