package phoenix.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import phoenix.security.JwtAuthenticationFilter;
import phoenix.security.JwtUtil;
import phoenix.handler.OAuth2SuccessHandler;
import phoenix.service.CustomOAuth2UserService;
import phoenix.service.MembersService;

/**
 * <h2>SecurityConfig</h2>
 * <p>
 * Spring Security 전역 보안 설정 클래스.<br>
 * JWT 기반 인증과 OAuth2(Google, GitHub, Facebook) 로그인을 통합 관리.
 * </p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>모든 HTTP 요청의 인증·인가 정책 정의</li>
 *   <li>JWT 인증 필터 등록 및 커스텀 OAuth2 로그인 로직 적용</li>
 *   <li>OAuth2 로그인 성공 시 {@link OAuth2SuccessHandler} 실행</li>
 * </ul>
 *
 * <h3>보안 정책 요약</h3>
 * <ul>
 *   <li>회원가입, 로그인, 이메일 인증, 토큰 갱신, 소셜 로그인 URL 등은 비인증 접근 허용</li>
 *   <li>그 외 모든 요청은 JWT 또는 OAuth2 인증 필요</li>
 *   <li>CSRF, 기본 로그인폼, HTTP Basic 인증 비활성화</li>
 * </ul>
 *
 * @author Phoenix
 * @since 2025-10
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CustomOAuth2UserService customOAuth2UserService; // 추가된 커스텀 OAuth2UserService

    /** JwtAuthenticationFilter를 Bean으로 등록 */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Lazy MembersService membersService) {
        // @Lazy 주입으로 순환참조 방지
        return new JwtAuthenticationFilter(jwtUtil, membersService);
    }

    /**
     * <h3>보안 필터 체인 설정</h3>
     * <p>
     * - URL별 접근 권한 설정<br>
     * - JWT 인증 필터 등록<br>
     * - OAuth2 로그인 설정
     * </p>
     *
     * @param security HttpSecurity 설정 객체
     * @return SecurityFilterChain 완성된 보안 필터 체인
     * @throws Exception 구성 중 발생한 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {

        security
                // =============================
                // 기본 보안 설정 비활성화 (REST용)
                // =============================
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // =============================
                // URL 접근 제어
                // =============================
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/members/signup",
                                "/members/login",
                                "/members/email/send",
                                "/members/verify-email",
                                "/members/token/refresh",
                                "/members/logout",
                                "/oauth/**" // 소셜 로그인 허용
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // =============================
                // OAuth2 로그인 설정
                // =============================
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(user -> user.userService(customOAuth2UserService)) // 커스텀 서비스 등록
                        .successHandler(oAuth2SuccessHandler) // 로그인 성공 후 후처리
                        .defaultSuccessUrl("/oauth/success", true) // 성공 시 이동
                        .failureUrl("/oauth/failure") // 실패 시 이동
                )

                // =============================
                // JWT 인증 필터 등록
                // =============================
                .addFilterBefore(jwtAuthenticationFilter(null),
                        UsernamePasswordAuthenticationFilter.class);

        return security.build();
    }

    /**
     * <h3>AuthenticationManager 빈 등록</h3>
     * <p>
     * - 로그인 시 인증 객체를 생성하는 핵심 매니저<br>
     * - 내부적으로 UserDetailsService를 통해 사용자 정보를 검증
     * </p>
     *
     * @param configuration 인증 설정 객체
     * @return AuthenticationManager 인스턴스
     * @throws Exception 생성 실패 시 예외
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * <h3>비밀번호 암호화기 등록</h3>
     * <p>
     * - BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해시화.<br>
     * - 회원가입 시 해시화, 로그인 시 비교 검증에 사용.
     * </p>
     *
     * @return PasswordEncoder (BCryptPasswordEncoder)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
