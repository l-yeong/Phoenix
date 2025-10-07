package phoenix.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import phoenix.security.JwtAuthenticationFilter;
import phoenix.security.JwtUtil;

/**
 *   Spring Security 핵심 설정 클래스
 * - 모든 HTTP 요청의 접근 권한, 인증 방식, 필터 체인 등을 설정
 * - Controller 이전 단계에서 동작하는 보안 관문 역할
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final AuthenticationConfiguration authenticationConfiguration;


    /**
     *   모든 요청에 대해 동작하는 “보안 필터 체인” 설정
     * - 로그인/회원가입 등은 누구나 접근 가능 (permitAll)
     * - 그 외 요청은 인증 필요 (authenticated)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity security) throws Exception {

        security
                .csrf(csrf -> csrf.disable()) // CSRF(사이트 간 요청 위조) 보호 비활성화
                .formLogin(form -> form.disable()) // 기본 로그인 폼 비활성화
                .httpBasic(basic -> basic.disable()) // 기본 인증창 비활성화 (REST API 사용을 위함)

                // 접근 제한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/members/signup" , "/members/login" , "/email/**").permitAll() // 인증 불필요
                        .requestMatchers("/admin//**").hasRole("ADMIN")  // ADMIN만 접근 가능
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
                )

                // JWT 필터 연결
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil) , UsernamePasswordAuthenticationFilter.class);

        return security.build();

    } // func e


    /**
     *   로그인 시 인증을 처리하는 핵심 매니저
     * - UserDetailsService를 통해 사용자 정보를 가져오고
     * - 비밀번호 일치 여부를 확인하는 역할
     */
    // authenticationManager 등록 (로그인 시 인증 객체 생성용)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception{
        return configuration.getAuthenticationManager();
    } // func e

    /**
     *   비밀번호 암호화기 (BCrypt 사용)
     * - 회원가입 시 비밀번호 해시화
     * - 로그인 시 비밀번호 검증
     */
    // 비밀번호 암호화(BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    } // func e


} // class e
