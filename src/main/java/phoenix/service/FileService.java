package phoenix.service;

import com.opencsv.CSVReader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import phoenix.model.dto.GameDto;
import phoenix.util.TicketsQR;

import java.io.*;
import java.nio.charset.StandardCharsets;
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
    //private Map<String , Map<String ,String >> gameMap;



    private String baseDir = System.getProperty("user.dir"); //루트 디렉터리 경로
    private String uploadPath = baseDir + "/src/main/resources/static/upload/"; //QR 이미지 저장 경로

    public String saveQRImg(String url) {
        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            // 날짜+UUID 조합 파일명 지정
            String date = java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

            // 6자리 짧은 UUID 생성
            String uuid = java.util.UUID.randomUUID()
                    .toString()
                    .replace("-","")
                    .substring(0,6);

            // 날짜+UUID 파일명
            String fileName = date +"_"+uuid+"_qr.png";
            Path output = dir.resolve(fileName);

            // QR 텍스트를 직접 전달
            byte[] png = ticketsQR.TicketQrCode(url, 200);
            Files.write(output, png);

            // 웹 접근 경로 반환
            return "/upload/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("QR 파일 저장 실패", e);
        }//catch end
    }//func end



    public boolean deleteQRImg(String fileImgDelete) {
        try {
            if ( fileImgDelete == null || !fileImgDelete.startsWith("/upload/")) return false;
            String filePath = uploadPath + fileImgDelete.replace("/upload/", "");
            File file = new File(filePath);
            return file.exists() && file.delete();
        } catch (Exception e) {
            System.out.println("[QR 파일 삭제 실패] " + fileImgDelete + " | " + e.getMessage());
        }//catch end
        return false;
    }//func end


    ///**
    // * 서비스 생성시 csv파일 읽어오는 기능
    // *
    // */
    //@PostConstruct
    //private void init(){
    //    gameMap = new HashMap<>();
    //    System.out.println("CSV");
    //    loadCsv();
    //}// func end
    //
    ///**
    // * CSV 파일 읽어서 gameMap에 저장
    // */
    //private void loadCsv(){
    //    try{
    //        CSVReader reader = new CSVReader(new FileReader("src/main/resources/static/games.csv"));
    //        String[] headers = reader.readNext(); // 첫 줄 : 컬럼명
    //        String[] line;
    //        while ((line = reader.readNext()) != null ){
    //            Map<String,String> row = new HashMap<>();
    //            for (int i = 0; i < headers.length; i++){ // 0번째는 번호(gno)
    //                row.put(headers[i],line[i]);
    //            }// for end
    //            gameMap.put(line[0] , row);
    //        }
    //    } catch (Exception e){
    //        e.printStackTrace();;
    //    }// try end
    //}// func end

    /**
     * 특정 경기번호의 경기내용 조회
     *
     * @param gno 경기번호
     * @return Map 경기정보
     */
    public GameDto getGame(int gno){
        List<GameDto> list = loadGames();
        for (GameDto dto : list){
            if (dto.getGno() == gno){
                return dto;
            }// if end
        }// for end
        return null;
    }// func end


    /**
     * 티켓 만료기간 위한 메소드
     * 공백 제거 유틸 (null-safe trim)
     */
    private static String trimOrNull(String s) {
        return (s == null) ? null : s.trim();
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
        List<GameDto> list = loadGames();
        //gameMap(CSV 에서 읽은 전체 경기 데이터)
        for (GameDto dto : list) {
            try {

                if(dto.getDate()==null || dto.getTime()==null){ //date,time 값 null 일경우 NullPointer 예외처리발생 방지
                    System.out.println("[getExpiredGames] date/time 누락 gno ="+dto.getGno());
                    continue;
                }//if end

                LocalDateTime gameDT = LocalDateTime.of(dto.getDate(),dto.getTime());
                if(gameDT.isBefore(now)){
                    expired.add(dto.getGno());
                }//if end

            } catch (Exception e) {
                System.out.println("[getExpiredGames] 날짜/시간 파싱 실패 gno=" + dto.getGno() + dto.getDate() + dto.getTime());
            }//catch end
        }//for end
        return expired;
    }//func end

    private static final String CSV_PATH = "src/main/resources/static/games.csv";

    public List<GameDto> loadGames() {
        List<GameDto> games = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(CSV_PATH), StandardCharsets.UTF_8))) {

            // 1) 헤더 스킵 (+ BOM 방지)
            String header = br.readLine();
            if (header != null && header.startsWith("\uFEFF")) {
                header = header.substring(1); // BOM 제거
            }

            String line;
            while ((line = br.readLine()) != null) {
                // 빈 줄 스킵 + CR 제거
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);

                // ⭐ 뒤쪽 빈 컬럼 보존: `...,17:00,,` 같은 행도 컬럼 9개 보장
                String[] arr = line.split(",", -1);

                // 컬럼 수 점검(안전)
                if (arr.length < 9) {
                    System.out.println("[CSV] 컬럼 부족으로 스킵: " + line);
                    continue;
                }

                try {
                    // trim + "null"/빈문자 → null 정규화 헬퍼
                    java.util.function.Function<String, String> norm = s -> {
                        if (s == null) return null;
                        String v = s.trim();
                        if (v.isEmpty()) return null;
                        if ("null".equalsIgnoreCase(v)) return null;
                        return v;
                    };

                    Integer gno = Integer.parseInt(arr[0].trim());
                    String homeTeam     = norm.apply(arr[1]);
                    String homePitcher  = norm.apply(arr[2]);
                    String awayTeam     = norm.apply(arr[3]);
                    String awayPitcher  = norm.apply(arr[4]);
                    LocalDate date      = LocalDate.parse(arr[5].trim());     // YYYY-MM-DD
                    LocalTime time      = LocalTime.parse(arr[6].trim());     // HH:mm
                    String result       = norm.apply(arr[7]);                 // "", "null" → null
                    String score        = norm.apply(arr[8]);                 // "", "null" → null

                    GameDto game = GameDto.builder()
                            .gno(gno)
                            .homeTeam(homeTeam)
                            .homePitcher(homePitcher)
                            .awayTeam(awayTeam)
                            .awayPitcher(awayPitcher)
                            .date(date)
                            .time(time)
                            .result(result)
                            .score(score)
                            .build();

                    games.add(game);
                } catch (Exception e) {
                    System.out.println("[CSV] 파싱 오류로 스킵: " + line + " | err=" + e.getMessage());
                }
            }

            System.out.println("[CSV] 파일로드 완료: " + games.size() + "건");
        } catch (IOException e) {
            System.out.println("[CSV] 파일 예외: " + e.getMessage());
        }

        return games;
    }



}//class end