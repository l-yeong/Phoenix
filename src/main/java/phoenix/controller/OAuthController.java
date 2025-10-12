package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import phoenix.model.dto.MembersDto;
import phoenix.service.SocialAuthService;

/**
 * OAuth 로그인 성공 후 JWT 발급 처리 컨트롤러
 */
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final SocialAuthService socialAuthService;

    /** 로그인 성공 시 */
    @GetMapping("/success")
    public String success(@AuthenticationPrincipal OAuth2User oAuth2User) {
        // 플랫폼 구분
        String provider = oAuth2User.getAttribute("iss") != null ? "google" :
                (oAuth2User.getAttribute("id") != null && oAuth2User.getAttribute("login") != null ? "github" : "facebook");

        // provider별 사용자 정보 추출
        String providerId = oAuth2User.getAttribute("sub") != null ? oAuth2User.getAttribute("sub") : oAuth2User.getAttribute("id");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // DB 존재여부 확인 후 JWT 발급 or 신규 등록
        String token = socialAuthService.socialLogin(provider, providerId);
        if (token == null) {
            // 신규 회원이면 자동 회원가입 처리 or 프론트 안내
            MembersDto dto = new MembersDto();
            dto.setMname(name);
            dto.setEmail(email);
            dto.setProvider(provider);
            dto.setProvider_id(providerId);
            dto.setEmail_verified(true);
            socialAuthService.socialSignUp(dto);
            return "[신규 등록 완료] 이메일: " + email;
        }
        return "[로그인 성공] JWT: " + token;
    }

    /** 로그인 실패 시 */
    @GetMapping("/failure")
    public String failure() {
        return "[OAuth 실패] 로그인 중 오류 발생";
    }
}
