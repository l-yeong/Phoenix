package phoenix.service;

import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import phoenix.util.TicketsQR;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileService {
    private final TicketsQR ticketsQR;
    private Map<String , Map<String ,String >> gameMap;


    private String baseDir = System.getProperty("user.dir"); //루트 디렉터리 경로
    private String uploadPath = baseDir + "/src/main/resources/static/upload/"; //QR 이미지 저장 경로

    public String saveQRImg(Object data) {
        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // Map → JSON 문자열 변환 (text 키로 감싸지 않음)
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = om.writeValueAsString(data);

            String fileName = UUID.randomUUID() + "_qr.png";
            Path output = dir.resolve(fileName);

            // QR 텍스트를 직접 전달
            byte[] png = ticketsQR.TicketQrCode(json, 200);
            Files.write(output, png);

            return "/upload/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("QR 파일 저장 실패", e);
        }
    }

    /**
     * 서비스 생성시 csv파일 읽어오는 기능
     *
     */
    @PostConstruct
    private void init(){
        gameMap = new HashMap<>();
        loadCsv();
    }// func end

    /**
     * CSV 파일 읽어서 gameMap에 저장
     */
    private void loadCsv(){
        try{
            CSVReader reader = new CSVReader(new FileReader("src/main/resources/static/games.csv"));
            String[] headers = reader.readNext(); // 첫 줄 : 컬럼명
            String[] line;
            while ((line = reader.readNext()) != null ){
                Map<String,String> row = new HashMap<>();
                for (int i = 0; i < headers.length; i++){ // 0번째는 번호(gno)
                    row.put(headers[i],line[i]);
                }// for end
                gameMap.put(line[0] , row);
            }
        } catch (Exception e){
            e.printStackTrace();;
        }// try end
    }// func end

    /**
     * 특정 경기번호의 경기내용 조회
     *
     * @param gno 경기번호
     * @return Map 경기정보
     */
    public Map<String,String> getGame(int gno){
        return gameMap.get(gno);
    }// func end


    /**
     * 티켓 만료기간 위한 메소드
     * 공백 제거 유틸 (null-safe trim)
     */
    private static String trimOrNull(String s) {
        return (s == null) ? null : s.trim();
    }


    /**
     * 티켓 만료기간 위한 메소드
     * 날짜 문자열 파서 (다양한 포맷 지원)
     * 지원 포맷: "2025-10-20", "2025/10/20", "20251020"
     */
    private static LocalDate parseDate(String s) {
        List<DateTimeFormatter> fs = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE    // 2025-10-20
        );
        for (DateTimeFormatter f : fs) {
            try { return LocalDate.parse(s, f); } catch (Exception ignore) {}
        }
        throw new IllegalArgumentException("지원하지 않는 날짜 포맷: " + s);
    }//func end

    /**
     * 티켓 만료기간 위한 메소드
     * 시간 문자열 파서 (HH:mm 또는 HH:mm:ss 지원)
     */
    private static LocalTime parseTime(String s) {
        List<DateTimeFormatter> fs = List.of(
                DateTimeFormatter.ofPattern("HH:mm")    // 19:05
        );
        for (DateTimeFormatter f : fs) {
            try { return LocalTime.parse(s, f); } catch (Exception ignore) {}
        }
        throw new IllegalArgumentException("지원하지 않는 시간 포맷: " + s);
    }


    /**
     * 티켓 만료기간 위한 헬퍼메소드
     * 지난 경기(gno) 목록 추출
     * - 오늘(LocalDate.now()) 기준으로 날짜가 지난 경기만 추출
     * - time이 비어 있으면 당일 23:59:59로 간주
     */
    public List<Integer> getExpiredGames() {
        List<Integer> expired = new ArrayList<>();
        // KST 현재 시각
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        //gameMap(CSV 에서 읽은 전체 경기 데이터)
        for (Map.Entry<String, Map<String, String>> entry : gameMap.entrySet()) {
            String gnoStr = entry.getKey();
            Map<String, String> row = entry.getValue();

            // CSV 컬럼 데이터 추출
            String dateStr = trimOrNull(row.get("date"));
            String timeStr = trimOrNull(row.get("time"));

            try {
                    LocalDate d = parseDate(dateStr);
                    LocalTime t = (timeStr == null || timeStr.isEmpty())
                            ? LocalTime.of(23, 59, 59) //CSV 경기날짜 공백일 경우 23:59:59에 티켓 만료
                            : parseTime(timeStr);
                    LocalDateTime gameDT = LocalDateTime.of(d,t);
                    if(gameDT.isBefore(now)){
                        expired.add(Integer.parseInt(gnoStr));
                    }//if end
            } catch (Exception e) {
                System.out.println("[getExpiredGames] 날짜/시간 파싱 실패 gno=" + gnoStr + dateStr + timeStr);
            }//catch end
        }//for end
        return expired;
    }//func end

}//class end