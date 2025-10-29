package phoenix.chatbot;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GeminiResponse {
    private List<Candidate> candidates;

    @Data
    @NoArgsConstructor
    public static class Candidate {
        private Message content;
    }
}
