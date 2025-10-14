package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스.
 * <p>Redis를 활용해 토큰을 저장, 검증, 삭제한다.</p>
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    /** Redis 키 prefix */
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String BLACKLIST_PREFIX = "blacklist";

    /**
     * Refresh Token 저장
     * @param identifier 회원 아이디
     * @param token 리프레시 토큰
     * @param minutes 만료 시간(분 단위)
     */
    public void saveRefreshToken(String identifier, String token, long minutes) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + identifier, token, minutes, TimeUnit.MINUTES);
    } // func e

    /**
     * 저장된 Refresh Token 검증
     * @param identifier 회원 아이디
     * @param token 요청에서 받은 토큰
     * @return 유효 여부
     */
    public boolean validateRefreshToken(String identifier, String token) {
        String saved = redisTemplate.opsForValue().get(REFRESH_PREFIX + identifier);
        return saved != null && saved.equals(token);
    } // func e

    /**
     * Refresh Token 삭제 (로그아웃 시)
     * @param identifier 회원 아이디
     */
    public void deleteRefreshToken(String identifier) {
        redisTemplate.delete(REFRESH_PREFIX + identifier);
    }

    /**
     * Access Token 블랙리스트 등록
     * @param accessToken
     */
    public void addBlacklist(String accessToken){
        redisTemplate.opsForValue().set("blacklist:" + accessToken , "logout" , 10 , TimeUnit.MINUTES);
    } // func e

} // class e
