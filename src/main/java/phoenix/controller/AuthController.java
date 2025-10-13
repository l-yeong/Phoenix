package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import phoenix.security.JwtUtil;
import phoenix.service.TokenService;
import phoenix.util.ApiResponseUtil;

import java.util.Map;

/**
 * 인증 관련 컨트롤러.
 * <p>Access/Refresh Token 재발급 및 로그아웃 API를 제공한다.</p>
 */
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    /**
     * Refresh Token을 이용한 Access Token 재발급 API.
     * @param request { "mid": "user123", "refresh_token": "yyy" }
     * @return 새 Access Token
     */
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponseUtil<?>> refreshAccessToken(@RequestBody Map<String, String> request) {
        String mid = request.get("mid");
        String refreshToken = request.get("refresh_token");

        if (tokenService.validateRefreshToken(mid, refreshToken)) {
            String newAccessToken = jwtUtil.generateToken(mid);
            return ResponseEntity.ok(new ApiResponseUtil<>(true, "Access Token 재발급 완료", newAccessToken));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponseUtil<>(false, "Refresh Token이 유효하지 않습니다.", null));
    }

    /**
     * 로그아웃 API.
     * <p>Redis에서 Refresh Token을 삭제한다.</p>
     * @param request { "mid": "user123" }
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseUtil<?>> logout(@RequestBody Map<String, String> request) {
        String mid = request.get("mid");
        tokenService.deleteRefreshToken(mid);
        return ResponseEntity.ok(new ApiResponseUtil<>(true, "로그아웃 완료", null));
    }
}
