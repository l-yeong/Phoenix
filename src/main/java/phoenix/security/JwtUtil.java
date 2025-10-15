package phoenix.security;

import io.jsonwebtoken.*;                   // JWT 관련 클래스들 (토큰 생성, 파싱 등)
import io.jsonwebtoken.security.Keys;       // 시크릿 키를 HMAC-SHA 알고리즘용 Key 객체로 변환
import org.springframework.beans.factory.annotation.Value;  // application.properties 값 주입용
import org.springframework.stereotype.Component;
import phoenix.model.dto.MembersDto;

import java.util.Date;

/*
*   JWT 토큰 생성 및 검증 유틸 클래스
*   - 로그인 성공시 토큰 발급
*   - 토큰 유효성 검증 및 아이디 추출 기능 제공
* */
@Component // 스프링 빈 등록 → 어디서든 @Autowired로 불러 쓸 수 있음
public class JwtUtil {

    // application.properties 파일에서 jwt.secret 값을 가져옴
    @Value("${jwt.secret}")
    private String secret;

    private final long accessTokenVaildity = 60 * 60 * 1000L; // 액세스 토큰 유효기간 (1시간)
    private final long refreshTokenVaildity = 7 * 24 * 60 * 60 * 1000L; // 리프레시 토큰 유효기간 (7일)

    /**
     *  Access Token 생성 메소드
     *  @param member 회원 DTO
     *  @return JWT Access Token
     */
    public String generateToken(MembersDto member){
        Date now = new Date();  // 현재 시각을 now에 저장
        Date expiry = new Date( now.getTime() + accessTokenVaildity ); // 만료 시각 (현재 시각 + 1시간)

        return Jwts.builder()   // JWT 생성 시작
                .subject(member.getMid())   // 토큰의 subject(내용, 즉 사용자 식별자)로 identifier 저장
                .issuedAt(now)  // 토큰 발급 시간(iat)
                .expiration(expiry) // 토큰 만료 시간(exp)
                .claim("mno" , member.getMno()) // claim() : 커스텀 정보 꺼낼 수 있는 메소드
                .signWith(Keys.hmacShaKeyFor(secret.getBytes())) // 서명(Signature) : secret 값을 HMAC-SHA256용 Key로 변환해서 서명
                .compact(); // 최종적으로 JWT 문자열로 변환 (header.payload.signature 구조)
    } // func e


    /**
     *  Refresh Token 생성 메소드
     * @param identifier
     * @return JWT Refresh Token
     * */
    public String generateRefreshToken( String identifier ){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenVaildity); // 만료 시각 (현재 시각 + 7일)

        // JWT 생성 로직은 Access 토큰과 동일 , 유효기간만 다름
        return Jwts.builder()
                .subject(identifier)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    } // func e

    /**
     *  토큰 유효성 검증
     *  @param token JWT 문자열
     *  @return boolean (true : 유효 , false : 만료 / 위조 )
     */
    public boolean validateToken(String token ){
        try{
            Jwts.parser()       // parser() : JWT 파서 빌더 객체 생성
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes())) // secret 키를 사용해 서명 검증 준비
                    .build()    // 실제 파서 빌드
                    .parseSignedClaims(token);  // 서명 검증 및 payload 추출 시도
                    return true;    // 예외가 없으면 유효한 토큰
        }catch (JwtException e ){
            return false; // JwtException 발생 → 서명 위조, 만료, 구조 오류 등
        }
    } // func e

    /**
     *  토큰에서 아이디 추출
     *  @param token JWT 문자열
     *  @return identifier (mid or email)
     * */
    public String getMid( String token){
        try {
            // 토큰을 파싱하고 payload(subject 부분)를 꺼내서 반환
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload() // payload(내용부)에서 subject 값(mid)을 가져옴
                    .getSubject();

        }catch (ExpiredJwtException e ){
            // 만료된 토큰에서도 subject는 추출 가능
            return e.getClaims().getSubject();
        }catch (JwtException e ){
            // 구조 오류 등
            return null;
        }

    } // func e

    // 토큰 전체 claims 반환 메소드 , payload 전체
    public Claims getClaims(String token){
        try {
            return Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        }catch (ExpiredJwtException e ){
            return e.getClaims(); // 만료되어도 claims는 추출 가능
        }catch (JwtException e ){
            return null;
        }
    } // func e

    // 특정 claim만 꺼내는 메소드
    public Object getClaim(String token , String key){
        Claims claims = getClaims(token);
        return (claims != null ) ? claims.get(key) : null;
    } // func e


} // class e
