package phoenix.util;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 *  공통 API 응답 클래스
 *  - 성공 여부 , 메시지 , 데이터(payload)를 함께 반환
 * @param <T> 응답 데이터 타입
 * */
@Data
@AllArgsConstructor
public class ApiResponseUtil<T> {
    private boolean success;    // 요청 성공 여부
    private String message;     // 설명 메시지
    private T data;             // 응답 데이터( DTO , JWT , List 등 )

} // class e
