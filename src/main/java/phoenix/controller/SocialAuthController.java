package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.security.JwtUtil;
import phoenix.service.SocialAuthService;
import phoenix.util.ApiResponseUtil;
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

    /**
     * 소셜 로그인 요청
     * <p>provider(provider), provider_id(provider 고유ID)를 받아 JWT 발급</p>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseUtil<?>> socialLogin(@RequestBody Map<String, String> req) {
        String provider = req.get("provider");
        String providerId = req.get("provider_id");

        String accessToken = socialAuthService.socialLogin(provider, providerId);
        if (accessToken == null) {
            // 신규 회원 분기
            return ResponseEntity.ok(new ApiResponseUtil<>(false, "신규 회원입니다. 추가정보 입력 필요",
                    Map.of("provider", provider, "provider_id", providerId)));
        }

        // JWT 안에서 mid 추출
        String mid = jwtUtil.getMid(accessToken);
        Integer mno = (Integer) jwtUtil.getClaim(accessToken , "mno");

        Map<String, Object> tokenInfo = Map.of(
                "access_token", accessToken,
                "mid" , mid,    // mid 포함
                "mno" , mno,        // mno 포함
                "token_type", "Bearer",
                "expires_in", 3600
        );

        return ResponseEntity.ok(new ApiResponseUtil<>(true, "로그인 성공", tokenInfo));
    }

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
    }
}
