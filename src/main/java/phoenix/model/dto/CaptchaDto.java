package phoenix.model.dto;


import lombok.*;

public class CaptchaDto {
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class NewCaptchaResponse {
        private String token;      // 서버가 만든 식별자
        private String imageBase64; // data:image/png;base64,...
    }
    @Data
    public static class VerifyRequest {
        private String token;
        private String answer; // 사용자가 입력한 문자
    }
    @Data @AllArgsConstructor @NoArgsConstructor
    public static class VerifyResponse {
        private int ok;
    }
}
