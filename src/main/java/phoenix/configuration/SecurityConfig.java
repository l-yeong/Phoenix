package phoenix.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
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
 *
 * SecurityFilterChain을 정의하는 설정 클래스
 * Spring Boot가 시작 될때 :
 *  @Configuration 클래스들이 스캔됨
 *  SecurityConfig 안의 @Bean 메서드들이 실행됨
 *  그 결과 SecurityFilterChain 이라는 객체가 스프링 컨테이너에 등록됨
 *
 * 1. 클라이언트 요청 도착 예시 "http://localhost:8080/members/info"
 * 2. DispatcherServlet 앞단에 "보안 필터 체인" 존재
 *      - Spring Boot는 서블릿 필터로 등록된 DelegatingFilterProxy -> FilterChainProxy(SecurityFilterChain)을 통해 모든 HTTP 요청을 먼저 가로챔
 *      - 즉, SecurityConfig 에서 .build()로 만든 SecurityFilterChain이 FilterChainProxy에 의해 자동 등록되어 모든 요청을 가로채기 함.
 * 3. FilterChain 내부에서 다음과 같은 보안 필터들이 순서대로 실행
 *      1) CorsFilter : CORS 정책 검사
 *      2) CsrfFilter : CSRF 토큰 검증
 *      3) UsernamePasswordAuthenticationFilter : 폼 로그인 시 사용자 인증
 *      4) JwtAuthenticationFilter : JWT 토큰 검증(프로젝트에서 직접 추가한 필터)
 *      5) SecurityContextPersistenceFilter : 인증정보를 SecurityContext에 저장
 *      6) ExceptionTranslationFilter : 인증/인가 예외 처리
 *      7) FilterSecurityInterceptor : URL 권한 체크 및 접근제어 실행
 *      - 즉 SecurityConfig의 설정은 이 필터들이 어떻게 작동할지를 미리 정의한 것
 *
 *
 */
@Configuration // 스프링 설정 클래스임을 표시 , 여기서 등록한 @Bean 들을 컨테이너가 관리
@RequiredArgsConstructor // final 필드들로 생성자 롬복이 자동 생성(의존성 주입)
public class SecurityConfig {

    private final JwtUtil jwtUtil; // JWT 생성/검증 유틸
    private final AuthenticationConfiguration authenticationConfiguration; // 프레임워크가 만든 인증구성 접근용(여기서 AuthenticationManager 얻음)
    private final OAuth2SuccessHandler oAuth2SuccessHandler; // OAuth2 로그인 성공 뒤 후처리 핸들러
    private final CustomOAuth2UserService customOAuth2UserService; // 추가된 커스텀 OAuth2UserService

    /** JwtAuthenticationFilter를 Bean으로 등록 */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Lazy MembersService membersService) {
        // @Lazy 주입으로 순환참조 방지
        return new JwtAuthenticationFilter(jwtUtil, membersService);
        // 요청마다 헤더의 JWT를 검증해 SecurityContext에 인증 정보 넣는 커스텀 필드 생성
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
     *
     * ====== SecurityFilterChain이 하는일 (인증/인가) ======
     *
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {

        security
                // =============================
                // 기본 보안 설정 비활성화 (REST용)
                // =============================
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 , 세션-폼 기반이 아닌 REST(JWT)
                .formLogin(form -> form.disable()) // 스프링 기본 로그인 폼 비활성화(프론트/REST로만 로그인 처리하겠다는 뜻)
                .httpBasic(basic -> basic.disable()) // Basic 인증 비활성화(아이디/비번을 Base64로 보내는 방식 금지
                .logout(logout -> logout.disable()) // 로그아웃 비활성화 추가
                .cors(cors -> {}) // CORS 설정과 통합(CorsConfig 반영)

                // =============================
                // URL 접근 제어
                // =============================
                .authorizeHttpRequests(auth -> auth // URL별 인가 규칙 시작
                        .requestMatchers(HttpMethod.OPTIONS , "/**").permitAll() // 프리플라이트 요청 허용
                        .requestMatchers("/members/social/**").permitAll() // 소셜 로그인/회원가입 경로 허용
                        .requestMatchers(                                       // requestMatchers(...).permitAll() : 나열한 경로는 "인증 없이" 접근 허용
                                "/members/signup",
                                "/members/login",
                                "/members/email/send",
                                "/members/verify-email",
                                "/members/token/refresh",
                                "/members/logout",
                                "/tickets/**",
                                "/oauth/**" // 소셜 로그인 허용

                        ).permitAll()
                        .anyRequest().authenticated() // 나머지는 전부 인증필요(JWT 또는 OAuth2 로그인 성공 상태)
                )

                // =============================
                // OAuth2 로그인 설정
                // =============================
                .oauth2Login(oauth2 -> oauth2 // oauth2Login : OAuth2 클라이언트 로그인 활성화
                        .userInfoEndpoint(user -> user.userService(customOAuth2UserService)) // provider(gogle, git , facebook)에서 받아온 사용자 프로필 커스텀 로직으로 가공
                        .successHandler(oAuth2SuccessHandler) // 로그인 성공 시 직접 토큰 발급/리다이렉트 등 후처리 , 이 핸들러가 우선 응답 완료하면 defaultSuccessUrl은 실행되지 않을 수 있음
                        .failureUrl("/oauth/failure") // 실패 시 이동경로
                )

                // =============================
                // JWT 인증 필터 등록
                // =============================
                .addFilterBefore(jwtAuthenticationFilter(null), // 커스텀 jwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 삽입
                                                                              // 요청 초기에 토큰 검증해서 SecurityContext 채워넣기 위함 : ======> NPE 위험?
                        UsernamePasswordAuthenticationFilter.class);

        return security.build(); // 위 설정으로 SecurityFilterChain 인스턴스 생성해서 컨테이너에 등록
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
