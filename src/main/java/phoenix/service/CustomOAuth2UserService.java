package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * <h2>CustomOAuth2UserService</h2>
 * <p>
 * 소셜 로그인(OAuth2) 성공 후 사용자 정보를 가공하고,
 * provider / provider_id 정보를 추출하여 서비스 레이어로 전달하는 클래스.
 * </p>
 *
 * <h3>역할</h3>
 * <ul>
 *   <li>Spring Security의 {@link DefaultOAuth2UserService} 를 상속받아 확장</li>
 *   <li>Google, GitHub, Facebook 등 각 provider별 사용자 정보 파싱</li>
 *   <li>{@link SocialAuthService} 를 호출하여 DB 회원 여부 확인 및 JWT 발급 유도</li>
 * </ul>
 *
 * <h3>주요 흐름</h3>
 * <ol>
 *   <li>OAuth2 인증 성공 → provider 정보 추출 (google / github / facebook)</li>
 *   <li>provider_id 추출 (Google=sub / GitHub=id / Facebook=id)</li>
 *   <li>SocialAuthService.socialLogin(provider, providerId) 호출</li>
 *   <li>회원 존재 여부에 따라 JWT 발급 or 신규 회원 처리</li>
 * </ol>
 *
 * @author Phoenix
 * @since 2025-10
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialAuthService socialAuthService;

    /**
     * OAuth2 로그인 성공 후 호출되는 메서드
     *
     * @param userRequest OAuth2 로그인 요청 정보 (클라이언트 등록, AccessToken 등 포함)
     * @return OAuth2User OAuth2 사용자 정보 객체
     * @throws OAuth2AuthenticationException 인증 과정에서 발생한 예외
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // [1] 기본 사용자 정보 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // [2] 어떤 소셜 플랫폼인지 식별 (google / github / facebook)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        var attributes = oAuth2User.getAttributes();

        // [3] provider별 고유 ID 추출
        Object rawId = (attributes.get("sub") != null) ? attributes.get("sub") : attributes.get("id");
        String providerId = rawId != null ? rawId.toString() : null;

        // [4] DB 회원 여부 확인 및 JWT 발급 시도
        String token = socialAuthService.socialLogin(provider, providerId);
        if (token == null) {
            System.out.println("🟡 신규 회원 감지 - provider: " + provider + ", providerId: " + providerId);
        } else {
            System.out.println("🟢 기존 회원 JWT 발급 완료");
        }

        return oAuth2User;
    }
}
