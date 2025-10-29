package phoenix.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiRequest {
    private String systemInstruction;
    private List<Message> contents; // (대화 기록)
    private GenerationConfig generationConfig;
}
