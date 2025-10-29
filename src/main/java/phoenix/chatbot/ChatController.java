package phoenix.chatbot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.service.MembersService;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final GeminiService geminiService;
    private final MembersService membersService;

    // 대화 기록 (서버 재시작 시 초기화됨)
    private List<Message> chatHistory = new ArrayList<>();

    // ... (생성자 생략)

    @PostMapping
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        String query = request.getMessage();
        int mno = membersService.getLoginMember().getMno();

        // 1. 기본 페르소나 및 RAG 지침 설정 (모든 지침을 하나의 문자열로 결합)
        String fullInstruction = "당신은 '인천 피닉스' 야구팀의 경기 예매를 돕는 친절한 챗봇입니다. 당신의 지식은 오직 '인천 피닉스'에 한정되어 있습니다.";

        // 2. 사용자 쿼리에 필요한 실시간 데이터를 조회
        String dbData = geminiService.getDataForm(query, mno);

        // 3. RAG 데이터가 있다면 시스템 지침 보강
        if (dbData != null && !dbData.isEmpty()){
            // AI가 데이터를 참조하여 답변하도록 시스템 명령을 구성
            String ragInstruction = "\n\n[RAG 지침]: 사용자가 '경기 일정', '다음 경기', '잔여 좌석' 등의 질문을 했을 경우, 다음 [실시간 경기 데이터]를 **유일한 정보 출처**로 사용하여 답변하세요. " +
                    "[실시간 경기 데이터]에 내용이 있다면 절대로 '모른다'고 답하거나 다른 스포츠에 대해 되묻지 말고," +
                    " 제공된 데이터를 기반으로 친절하게 답변해야 합니다.\n\n[실시간 경기 데이터]\n" + dbData;

            fullInstruction += ragInstruction;
        }
        // 4. API에 전달할 최종 대화 기록 리스트 생성 (원본 chatHistory는 수정하지 않음)
        List<Message> contentsForApi = new ArrayList<>(chatHistory);

        // ⭐ 5. RAG 지침(fullInstruction)과 실제 사용자 쿼리를 하나의 'user' 메시지로 결합합니다.
        //    이것이 400 오류를 피하는 최종 해결책입니다.
        String augmentedQuery = fullInstruction + "\n\n사용자 질문: " + query;

        // 6. 증강된 사용자 메시지를 API 전송 리스트의 '마지막'에 추가합니다.
        //    * role: "user"만 사용하므로 API가 요구하는 유효한 역할 규칙을 준수합니다.
        contentsForApi.add(new Message("user", List.of(new Part(augmentedQuery))));

        // 7. Gemini Service 호출 (시스템 지침을 포함한 전체 리스트 전달)
        String geminiResponseText = geminiService.getGeminiResponse(contentsForApi);

        // 8. 서버 측 대화 기록(chatHistory)에 저장 (CLEAN HISTORY 유지)
        //    * chatHistory에는 다음 호출의 교차 규칙을 위해 오리지널 쿼리만 저장합니다.
        chatHistory.add(new Message("user", List.of(new Part(query)))); // User's original query
        chatHistory.add(new Message("model", List.of(new Part(geminiResponseText))));

        // 9. 프론트엔드에 응답 반환
        return new ChatResponse(geminiResponseText);
    }
}
