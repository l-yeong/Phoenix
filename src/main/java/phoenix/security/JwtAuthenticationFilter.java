package phoenix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;

import java.io.IOException;
import java.util.List;


/**
 *   JWT 인증 필터 (요청 1개당 1번만 실행)
 * - Authorization 헤더에 담긴 JWT를 꺼내서 검증하는 역할
 * - SecurityFilterChain 중간에 추가되어 Controller 진입 전 실행됨
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // JWT 검증/파싱용 유틸
    private final MembersService membersService; // DB에서 회원정보 조회용

    public JwtAuthenticationFilter(JwtUtil jwtUtil, MembersService membersService) {
        this.jwtUtil = jwtUtil;
        this.membersService = membersService;
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
        if(authHeader != null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7); // "Bearer " 이후의 실제 토큰만 추출

            // [3] JWT 유효성 검증
            if(jwtUtil.validateToken(token)){
                String mid = jwtUtil.getMid(token);

                // [4] DB에서 회원 객체 조회
                MembersDto member = membersService.findByMid(mid);

                if( member != null ) {
                    // [5] 인증 객체 생성 (Principal에 MembersDto 저장)
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(member, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

                    // [6] SecurityContext에 인증 정보 등록
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                }
            }
            System.out.println("Authorization 헤더: " + authHeader);
            System.out.println("토큰 추출: " + token);
            System.out.println("토큰 유효성: " + jwtUtil.validateToken(token));
            System.out.println("토큰 주체(mid): " + jwtUtil.getMid(token));

        } // if e
        System.out.println("Authorization 헤더 = " + request.getHeader("Authorization"));
        System.out.println("SecurityContext 인증 객체 = " + SecurityContextHolder.getContext().getAuthentication());

        // [4] 다음 필터로 요청 전달
        filterChain.doFilter(request , response);
    } // func e,

} // class e
