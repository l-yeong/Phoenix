package phoenix.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Spring Security가 제공하는 비밀번호 암호화용 클래스 (단방향 해시)

/**
 *  비밀번호 암호화 및 검증 유틸 클래스
 *  - BCrypt 방식 사용 (단방향 암호화)
 *  - 회원가입 시 비밀번호를 안전하게 저장하고,
 *    로그인 시 사용자가 입력한 값과 DB의 해시값을 비교함
 */
public class PasswordUtil {

    // BCryptPasswordEncoder 인스턴스 (static: 한 번만 생성)
    // BCrypt는 매번 다른 salt를 자동으로 섞어주기 때문에 보안이 강력함
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     *  비밀번호 암호화 메소드
     *  - 사용자가 입력한 원본 비밀번호(rawPassword)를
     *    BCrypt 해시로 인코딩해서 반환함
     * @param  rawPassword 입력 비밀번호
     * @return String (암호화된 비밀번호)
     * */
    public static String encode(String rawPassword){
        // encoder.encode() → 매번 새로운 salt가 포함된 해시 생성
        // 결과 예시: $2a$10$qW9K8YwE7m...
        return encoder.encode(rawPassword);
    } // func e

    /**
     *  비밀번호 일치 여부 확인
     *  - 로그인 시 사용자가 입력한 비밀번호(rawPassword)를
     *    DB에 저장된 암호화된 해시(encodePassword)와 비교함
     * @param rawPassword 입력 비밀번호
     * @param encodePassword DB저장 비밀번호 해시
     * @return boolean ( true : 일치)
     * */
    public static boolean matches(String rawPassword , String encodePassword ){
        // BCrypt 내부에서 rawPassword를 encodePassword의 salt로 재암호화해서 비교
        // 비밀번호가 맞으면 true, 틀리면 false
        return encoder.matches(rawPassword , encodePassword);
    } // func e



} // class e
