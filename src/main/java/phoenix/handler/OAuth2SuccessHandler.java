package phoenix.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import phoenix.model.dto.MembersDto;
import phoenix.security.JwtUtil;
import phoenix.service.SocialAuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2SuccessHandler
 * <p>
 * - Spring Security의 OAuth2 로그인 성공 시 실행되는 핸들러 클래스<br>
 * - Google / GitHub / Facebook 등 OAuth 로그인 후, 사용자 정보(attribute)를 받아 JWT 발급 및 리다이렉트 처리
 * </p>
 *
 * <h3>동작 흐름</h3>
 * <ol>
 *   <li>OAuth2 로그인 성공 → SecurityContext 에 OAuth2AuthenticationToken 저장</li>
 *   <li>토큰에서 provider, providerId, email 등 사용자 정보 추출</li>
 *   <li>SocialAuthService.socialLogin() 호출 → JWT 발급 or 신규 회원 판단</li>
 *   <li>JWT 발급 시: 프론트엔드로 토큰 포함 리다이렉트</li>
 *   <li>신규 회원 시: 추가정보 입력 페이지로 이동</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /** 소셜 로그인 로직 (JWT 발급 및 신규회원 분기) */
    private final SocialAuthService socialAuthService;
    private final JwtUtil jwtUtil;

    /**
     * OAuth2 로그인 성공 후 호출되는 콜백 메서드
     *
     * @param request        클라이언트 요청 객체
     * @param response       응답 객체 (리다이렉트 응답 생성)
     * @param authentication 인증 객체 (OAuth2AuthenticationToken 형태)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        // [1] OAuth2AuthenticationToken으로 캐스팅 (OAuth 사용자 정보 포함)
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        // [2] Google / GitHub / Facebook 등에서 반환된 사용자 정보(attribute) 가져오기
        Map<String, Object> attributes = token.getPrincipal().getAttributes();

        // [3] 로그인한 소셜 제공자 식별 (google / github / facebook)
        String provider = token.getAuthorizedClientRegistrationId();

        // [4] providerId 플랫폼별로 다르게 처리
        String providerId = null;
        switch (provider) {
            case "google" -> providerId = (String) attributes.get("sub"); //  Google: "sub"
            case "github" -> providerId = String.valueOf(attributes.get("id")); // GitHub: "id" (Integer → String 변환)
            case "facebook" -> providerId = String.valueOf(attributes.get("id")); //  Facebook: "id"
            default -> providerId = String.valueOf(attributes.get("id")); // 기타
        }

        // [5] 이메일 (플랫폼별 기본값 보정)
        String email = (String) attributes.get("email");
        if (email == null || email.isEmpty()) {
            email = provider + "_" + providerId + "@social.local"; // 기본값 세팅
        }

        // [6] 소셜 로그인 처리 로직 호출 (JWT 발급 or 신규 회원 판단)
        String jwt = socialAuthService.socialLogin(provider, providerId);

        // [7] 신규 회원이면 → 추가 정보 입력 페이지로 이동
        if (jwt == null) {
            String redirectUrl = String.format(
                    "http://localhost:5173/social/signup?email=%s&provider=%s&provider_id=%s",
                    URLEncoder.encode(email , StandardCharsets.UTF_8),
                    URLEncoder.encode(provider , StandardCharsets.UTF_8),
                    URLEncoder.encode(providerId , StandardCharsets.UTF_8)
            );
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
        // [8] 기존 회원이면 → JWT를 프론트엔드로 전달
        else {

            // JWT로 Authentication 생성 후 SecurityContext에 등록
            Authentication authentication1 = jwtUtil.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication1);

            // 회원 정보 조회
            MembersDto member = socialAuthService.findMemberByProvider(provider , providerId);

            // mid , mno 포함 리다이렉트 url 생성
            String redirectUrl = String.format(
                    "http://localhost:5173/social/success?token=%s&mid=%s&mno=%d" ,
                    URLEncoder.encode(jwt , StandardCharsets.UTF_8),
                    URLEncoder.encode(member.getMid() , StandardCharsets.UTF_8) ,
                    member.getMno()
            );

            getRedirectStrategy().sendRedirect(request , response , redirectUrl);
        }

    } // func e

} // class e
