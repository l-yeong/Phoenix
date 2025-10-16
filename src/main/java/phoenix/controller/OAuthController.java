package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.service.SocialAuthService;

/**
 * <h2>OAuthController</h2>
 * <p>
 * 소셜 로그인(OAuth2) 성공 및 실패 후의 후처리를 담당하는 컨트롤러.<br>
 * Spring Security에서 인증된 {@link OAuth2User} 정보를 기반으로,
 * 내부 회원 여부를 확인하고 JWT 발급 또는 신규 회원가입을 수행.
 * </p>
 *
 * <h3>주요 역할</h3>
 * <ul>
 *   <li>OAuth2 로그인 성공 시 사용자 정보 처리</li>
 *   <li>provider(provider, provider_id) 기반으로 DB 확인 및 토큰 발급</li>
 *   <li>회원이 없을 경우 신규 회원 자동 등록 처리</li>
 * </ul>
 *
 * <h3>리디렉션 경로</h3>
 * <ul>
 *   <li>/oauth/success - 로그인 성공 후 호출</li>
 *   <li>/oauth/failure - 로그인 실패 시 호출</li>
 * </ul>
 *
 * @author Phoenix
 * @since 2025-10
 */
@RestController
@RequestMapping("/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final SocialAuthService socialAuthService;

    /**
     * OAuth2 로그인 성공 시 호출되는 엔드포인트
     *
     * @param oAuth2User 인증된 OAuth2 사용자 객체
     * @param authentication Spring Security의 인증 정보
     * @return 로그인 성공/신규회원 등록 메시지
     */
    @GetMapping("/success/test")
    public String success(@AuthenticationPrincipal OAuth2User oAuth2User, Authentication authentication) {
        // [1] provider 식별 (google / github / facebook)
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();

        // [2] provider_id 추출 (Google=sub / GitHub=id / Facebook=id)
        Object rawProviderId = (oAuth2User.getAttribute("sub") != null)
                ? oAuth2User.getAttribute("sub")
                : oAuth2User.getAttribute("id");
        String providerId = rawProviderId != null ? rawProviderId.toString() : null;

        // [3] 사용자 정보
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        /**
         * [3-1] provider 별 기본값 보정 로직
         * GitHub: name, email 둘 다 null인 경우가 많음
         * Facebook: 이메일은 있을 수 있지만 이름이 null일 가능성 존재
         * Google: 대부분 제공하지만 혹시 몰라 기본값 세팅
         */
        switch (provider) {
            case "github" -> {
                if (name == null) name = (String) oAuth2User.getAttribute("login"); // GitHub의 'login' 값 사용
                if (name == null) name = "github_user_" + providerId;
                if (email == null) email = "github_" + providerId + "@github.local";
            }
            case "facebook" -> {
                if (name == null) name = "facebook_user_" + providerId;
                if (email == null) email = "facebook_" + providerId + "@facebook.local";
            }
            case "google" -> {
                if (name == null) name = "google_user_" + providerId;
                if (email == null) email = "google_" + providerId + "@gmail.local";
            }
            default -> {
                if (name == null) name = "social_user_" + providerId;
                if (email == null) email = provider + "_" + providerId + "@social.local";
            }
        }

        // [4] DB 회원 존재 여부 확인 및 JWT 발급
        String token = socialAuthService.socialLogin(provider, providerId);
        if (token == null) {
            // 신규 회원 등록 처리
            MembersDto dto = new MembersDto();
            dto.setMname(name);
            dto.setEmail(email != null ? email : provider + "_" + providerId + "@social.local");
            dto.setProvider(provider);
            dto.setProvider_id(providerId);
            dto.setEmail_verified(true);

            socialAuthService.socialSignUp(dto);
            return "[신규 등록 완료] 이메일: " + dto.getEmail();
        }

        return "[로그인 성공] JWT: " + token;
    }

    /**
     * OAuth2 로그인 실패 시 호출되는 엔드포인트
     *
     * @return 실패 메시지
     */
    @GetMapping("/failure/test")
    public String failure() {
        return "[OAuth 실패] 로그인 중 오류 발생";
    }
}
