package phoenix.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.security.JwtUtil;
import phoenix.service.SocialAuthService;
import phoenix.service.TokenService;
import phoenix.util.ApiResponseUtil;

import java.util.List;
import java.util.Map;

/**
 * SocialAuthController
 * <p>OAuth2(Google, GitHub, Facebook) 로그인 및 회원가입 API</p>
 */
@RestController
@RequestMapping("/members/social")
@RequiredArgsConstructor
public class SocialAuthController {

    private final SocialAuthService socialAuthService;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    /**
     * 소셜 로그인 (세션 기반)
     * - provider / provider_id로 기존 회원 조회
     * - 신규면 추가정보 필요 메시지 반환
     * - 기존 회원이면 SecurityContextHolder에 인증 등록
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseUtil<?>> socialLogin(@RequestBody Map<String, String> req,
                                                          HttpServletRequest httpRequest) {

        String provider = req.get("provider");
        String providerId = req.get("provider_id");

        // 소셜 로그인 시도
        MembersDto member = socialAuthService.findMemberByProvider(provider, providerId);

        // 신규 회원이면 추가정보 입력 필요
        if (member == null) {
            return ResponseEntity.ok(new ApiResponseUtil<>(false, "신규 회원입니다. 추가정보 입력 필요",
                    Map.of("provider", provider, "provider_id", providerId)));
        }

        // 기존 회원이면 세션 기반 인증 등록
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(member.getMid(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        // JWT 발급 → Redis 기록용으로만 사용
        String token = jwtUtil.generateToken(member);
        tokenService.saveRefreshToken(token , member.getMid(), 60 * 60 );

        Map<String, Object> data = Map.of(
                "mid", member.getMid(),
                "mno", member.getMno(),
                "provider", member.getProvider()
        );

        return ResponseEntity.ok(new ApiResponseUtil<>(true, "소셜 로그인 성공 (세션 저장 완료)", data));
    } // func e


    /**
     * 소셜 신규 회원가입
     * <p>OAuth 인증 완료 후 추가 정보 입력 시 호출</p>
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponseUtil<?>> socialSignUp(@RequestBody MembersDto dto) {
        boolean result = socialAuthService.socialSignUp(dto);
        if (result) {
            return ResponseEntity.ok(new ApiResponseUtil<>(true, "소셜 회원가입 완료", null));
        }
        return ResponseEntity.badRequest().body(new ApiResponseUtil<>(false, "회원가입 실패", null));
    } // func e

} // class e
