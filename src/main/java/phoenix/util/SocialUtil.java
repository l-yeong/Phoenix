package phoenix.util;

import org.springframework.stereotype.Component;

/**
 * <h2>SocialUtil</h2>
 * <p>
 * 소셜 로그인 관련 공통 유틸리티 클래스.<br>
 * provider_id 가공, 임시 전화번호 생성 등 재사용 가능한 헬퍼 메서드 제공.
 * </p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>provider_id 기반 유니크한 임시 전화번호 생성</li>
 *   <li>null-safe 문자열 처리</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 *   String phone = SocialUtil.generateTempPhone("214445432");
 *   // 결과: "000-4432"
 * </pre>
 *
 * @author Phoenix
 * @since 2025-10
 */
@Component
public class SocialUtil {

    /**
     * provider_id 뒤 4자리를 이용해 유니크한 임시 전화번호를 생성합니다.
     *
     * @param providerId OAuth2 provider의 고유 사용자 ID
     * @return String 형태의 임시 전화번호 (예: "000-4432")
     */
    public static String generateTempPhone(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return "000-9999";
        }
        String suffix = providerId.length() > 4
                ? providerId.substring(providerId.length() - 4)
                : providerId;
        return "000-" + suffix;
    }

    /**
     * 문자열이 null 또는 공백이면 기본값을 반환합니다.
     *
     * @param value 원본 문자열
     * @param defaultValue 기본값
     * @return null-safe 문자열
     */
    public static String nvl(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
