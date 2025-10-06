package phoenix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 *   JWT 인증 필터 (요청 1개당 1번만 실행)
 * - Authorization 헤더에 담긴 JWT를 꺼내서 검증하는 역할
 * - SecurityFilterChain 중간에 추가되어 Controller 진입 전 실행됨
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // JWT 검증/파싱용 유틸

    public JwtAuthenticationFilter(JwtUtil jwtUtil){
        this.jwtUtil = jwtUtil;
    }

    /**
     *  요청마다 자동 실행되는 메서드
     * - Authorization 헤더에서 JWT를 꺼내 유효성 검증
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request ,
                                    HttpServletResponse response ,
                                    FilterChain filterChain)
        throws ServletException , IOException {

        // [1] Authorization 헤더 추출
        String authHeader = request.getHeader("Authorization");

        // [2] "Bearer " 로 시작하는지 확인
        if(authHeader != null && authHeader.startsWith("Bearer")){
            String token = authHeader.substring(7); // "Bearer " 이후의 실제 토큰만 추출

            // [3] JWT 유효성 검증

        }

        // [4] 다음 필터로 요청 전달
        filterChain.doFilter(request , response);
    } // func e,

} // class e
