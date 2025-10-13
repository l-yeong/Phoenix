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
     * 로그아웃 API
     * @param authHeader
     * @return
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseUtil<?>> logout(@RequestHeader(value = "Authorization" , required = false) String authHeader ) {
        if(authHeader == null || !authHeader.startsWith("Bearer")){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseUtil<>(false , "Authorization 헤더가 없습니다." , null));
        }

        String accessToken = authHeader.substring(7);
        if(!jwtUtil.validateToken(accessToken)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseUtil<>(false , "유효하지 않은 Access Token입니다.", null));
        }

        // JWT에서 subject 추출 (mid 또는 email)
        String identifier = jwtUtil.getMid(accessToken);

        if (identifier == null || identifier.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseUtil<>(false, "토큰에 회원 식별 정보가 없습니다.", null));
        }

        // Redis RefreshToken 삭제 + AccessToken 블랙리스트 추가
        tokenService.deleteRefreshToken(identifier);
        tokenService.addBlacklist(accessToken);

        return ResponseEntity.ok(new ApiResponseUtil<>(true , "로그아웃 완료" , null));

    } // func e

} // class e
