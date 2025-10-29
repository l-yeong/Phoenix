package phoenix.chatbot;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import phoenix.model.dto.GameDto;
import phoenix.model.mapper.ZonesMapper;
import phoenix.service.FileService;
import phoenix.service.SeatCsvService;
import phoenix.service.SeatLockService;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {
    // [*]
    private final FileService fileService;
    private final SeatCsvService seatCsvService;
    private final SeatLockService seatLockService;
    private final WebClient webClient;
    private final ZonesMapper zonesMapper;

    // 날짜 포맷터 정의
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M월 d일");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 모델 엔드포인트는 상수로 선언 (WebClient 호출에 사용)
    final String MODEL_ENDPOINT = "/models/gemini-2.5-flash:generateContent";

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;



    public String getGeminiResponse(List<Message> chatHistory ) {
        System.out.println("GeminiService.getGeminiResponse");
        // Gemini API Request Body 구성
        GeminiRequest request = new GeminiRequest();

        // 2. 대화 기록 설정
        request.setContents(chatHistory); // 대화 내용을 contents 필드에 포함

        try {
            Mono<GeminiResponse> responseMono = webClient.post()
                    // URL을 문자열로 결합하여 WebClient에 전달
                    .uri(geminiApiUrl + MODEL_ENDPOINT + "?key=" + geminiApiKey)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class);

            // block() 메서드는 이제 명확하게 GeminiResponse를 반환합니다.
            GeminiResponse geminiResponse = responseMono.block(); // 비동기 호출을 동기적으로 처리

            if (geminiResponse != null && geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) {
                // 응답에서 텍스트 추출
                String textResponse = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
                return textResponse;
            } else {
                return "죄송합니다. Gemini로부터 응답을 받지 못했습니다.";
            }// if end
        } catch (Exception e) {
            System.err.println("Gemini API Request Error: " + e.getMessage());
            if (e instanceof WebClientResponseException) {
                WebClientResponseException wcE = (WebClientResponseException) e;
                System.err.println("Status: " + wcE.getStatusCode());
                // API 서버가 보내준 구체적인 오류 메시지 (400 원인)를 출력
                System.err.println("Response Body (API Error Details): " + wcE.getResponseBodyAsString());
            } else {
                e.printStackTrace(); // 일반 예외의 경우 스택 트레이스 출력
            }
            return "죄송합니다. AI 서비스에 문제가 발생했습니다.";
        }// try end
    }// func end

    public String getDataForm(String query , int mno){
        System.out.println("GeminiService.getDataForm");
        StringBuilder dbData = new StringBuilder();
        List<GameDto> games = fileService.loadGames(); // CSV 파일 로드

        List<GameDto> futureGames = games.stream()
                // 날짜와 시간이 유효한지 확인하고, 현재 시각보다 미래인 경기만 필터링
                .filter(game -> game.getDate() != null && game.getTime() != null)
                .filter(game -> {
                    try {
                        LocalDateTime gameDateTime = LocalDateTime.of(game.getDate(), game.getTime());
                        return gameDateTime.isAfter(LocalDateTime.now());
                    } catch (Exception e) {
                        log.error("Invalid date or time for game filtering: {}", game, e);
                        return false;
                    }
                })
                // 가장 가까운 미래 경기 순으로 정렬
                .sorted(Comparator.comparing(game -> LocalDateTime.of(game.getDate(), game.getTime())))
                .collect(Collectors.toList());

        if (query.contains("경기 일정") || query.contains("경기") || query.contains("다음 경기") || query.contains("빠른 경기") || query.contains("오늘 기준")) {
            // 2. 조회된 DTO를 문자열로 포맷하여 추가 (미래 경기만 사용)
            dbData.append(formatGameData(futureGames));
        }// if end
        if (query.contains("잔여 좌석") || query.contains("자리") || query.contains("좌석") || query.contains("구역")){
            // 잔여 좌석 조회는 가장 빠른 미래 경기 하나만 사용
            GameDto targetGame = futureGames.stream()
                    .findFirst() // 이미 정렬되었으므로 첫 번째가 가장 빠른 경기
                    .orElse(null);

            int targetGno = 0;
            if (targetGame != null){
                targetGno = targetGame.getGno();
            }// if end

            if (targetGame == null || targetGno == 0){
                dbData.append("현재 잔여 좌석 정보를 조회할 수 있는 예정된 경기가 없습니다.\\n");
            }else {
                dbData.append(String.format("### 현재 잔여 좌석 조회 대상 경기: %s vs %s 경기일시 : %s %s\n",
                        targetGame.getHomeTeam(),
                        targetGame.getAwayTeam(),
                        targetGame.getDate().format(DATE_FORMATTER),
                        targetGame.getTime().format(TIME_FORMATTER)));
                Set<Integer> snoSet = seatCsvService.getAllSeatSnos();
                List<Integer> snoList = snoSet.stream().toList();
                Map<Integer,String> seatStatusMap = seatLockService.getSeatStatusFor(targetGno,mno,snoList);
                dbData.append(formatSeatData(seatStatusMap));
            }// if end
        }// if end

        if (dbData.length() == 0) {
            // 관련된 특정 데이터가 없을 경우 기본 정보 제공
            return "";
        }// if end
        System.out.println("dbData : " + dbData);
        return dbData.toString();
    }// func end

    /**
     * GameDto 리스트를 Markdown 형식의 문자열로 변환합니다.
     */
    private String formatGameData(List<GameDto> games) {
        System.out.println("GeminiService.formatGameData");
        if (games == null || games.isEmpty()) {
            return "### 경기 일정: 조회된 일정이 없습니다.\n";
        }

        String header = "### 다음 경기 일정 (최대 3개)\n";
        String list = games.stream()
                .limit(3)
                .map(game -> String.format("- **%s** %s (%s) : 상대팀 %s",
                        game.getDate().format(DATE_FORMATTER),
                        game.getTime().format(TIME_FORMATTER),
                        game.getHomeTeam(), // 홈팀 포함 (인천피닉스)
                        game.getAwayTeam()))
                .collect(Collectors.joining("\t"));

        return header + list + "\n";
    }// func end

    /**
     * SeatDto 객체를 Markdown 형식의 문자열로 변환합니다.
     */
    private String formatSeatData(Map<Integer, String> seatStatusMap) {
        System.out.println("GeminiService.formatSeatData");
        if ( seatStatusMap == null || seatStatusMap.isEmpty()) {
            return "### 잔여 좌석 정보: 현재 조회 가능한 좌석 정보가 없습니다.\n";
        }// if end
        Map<String, Long> availableSeatsByZone = seatStatusMap.entrySet().stream()
                .filter(entry -> "AVAILABLE".equals(entry.getValue())) // 'AVAILABLE' 상태만 필터링
                // 2. sno를 zname(구역 이름)으로 변환하여 집계
                .collect(Collectors.groupingBy(
                        entry -> {
                            int sno = entry.getKey();
                            SeatCsvService.SeatCsvDto meta = seatCsvService.getMeta(sno);
                            if (meta != null) {
                                int zno = meta.getZno();
                                // ⭐ 이 부분이 zname을 반환합니다.
                                String zname = "";
                                String znoCheck = seatCsvService.getZoneName(zno);
                                if (znoCheck.equals("ZNO 10001")){ zname = "겨레"; }
                                if (znoCheck.equals("ZNO 10002")){ zname = "연우"; }
                                if (znoCheck.equals("ZNO 10003")){ zname = "성호"; }
                                if (znoCheck.equals("ZNO 10004")){ zname = "찬영"; }
                                if (znoCheck.equals("ZNO 10005")){ zname = "중앙테이블"; }
                                if (znoCheck.equals("ZNO 10006")){ zname = "외야자유"; }
                                return zname;
                            }
                            return "알 수 없는 구역";
                        },
                        Collectors.counting()
                ));
        // 3. 구역 이름(zname)별 잔여 좌석 수를 포맷팅하여 반환합니다.
        // LinkedHashMap을 사용하여 순서를 유지하거나 원하는 순서로 정렬합니다.
        String formattedList = availableSeatsByZone.entrySet().stream()
                .map(entry -> String.format("- **%s석 구역**: %d석", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));

        return String.format(
                "### 현재 구역별 잔여 좌석 현황\n" +
                        "%s\t",
                formattedList
        );

    }// func end

    private GeminiRequest createGeminiRequest( String systemInstruction , String userPrompt){
        System.out.println("GeminiService.createGeminiRequest");
        GeminiRequest request = new GeminiRequest();
        // 시스템 지침 설정
        request.setSystemInstruction(systemInstruction);

        // 사용자 프롬프트를 Message 객체로 변환하여 설정
        Message userContent = Message.builder()
                .role("user")
                .parts(List.of(Part.builder().text(userPrompt).build()))
                .build();
        request.setContents(List.of(userContent));
        // 기타 설정 (temperature 등)
        GenerationConfig config = new GenerationConfig();
        config.setTemperature(0.5);
        request.setGenerationConfig(config);

        return request;
    }// func end




}// class end