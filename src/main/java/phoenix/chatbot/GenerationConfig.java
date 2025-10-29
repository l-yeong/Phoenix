package phoenix.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GenerationConfig {
    private Double temperature; // 모델 출력의 창의성 제어 (0.0~1.0)
    private Integer maxOutputTokens; // 생성될 토큰의 최대 개수
    private List<String> stopSequences; // 특정 문자열에서 생성을 중단
}
