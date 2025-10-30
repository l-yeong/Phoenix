package phoenix.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.MembersDto;
import phoenix.security.JwtUtil;
import phoenix.service.MembersService;
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
    private final MembersService membersService;

//    /**
//     * Refresh Token을 이용한 Access Token 재발급 API.
//     * @param request { "mid": "user123", "refresh_token": "yyy" }
//     * @return 새 Access Token
//     */
//    @PostMapping("/token/refresh")
//    public ResponseEntity<ApiResponseUtil<?>> refreshAccessToken(@RequestBody Map<String, String> request) {
//        String mid = request.get("mid");
//        String refreshToken = request.get("refresh_token");
//
//        // refresh token 유효성 검증
//        if (! tokenService.validateRefreshToken(mid, refreshToken)) {
//            String newAccessToken = jwtUtil.generateToken(mid);
//            return ResponseEntity.ok(new ApiResponseUtil<>(true, "Access Token 재발급 완료", newAccessToken));
//        }
//        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                .body(new ApiResponseUtil<>(false, "Refresh Token이 유효하지 않습니다.", null));
//    }


    /**
     * 세션 기반 로그아웃 API (최종 단순화 버전)
     * - 별도의 쿠키/Redis 삭제 불필요
     * - SecurityContext 및 세션만 초기화
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponseUtil<?>> logout(HttpServletRequest request) {

        // 현재 세션 조회
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }

        // SecurityContext도 명시적으로 초기화 (권장)
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(new ApiResponseUtil<>(true, "로그아웃 완료", null));
    } // func e

} // class e
