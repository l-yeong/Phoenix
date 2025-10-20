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
     * 지난 경기(gno) 목록 추출
     * - 오늘(LocalDate.now()) 기준으로 날짜가 지난 경기만 추출
     * - CSV 컬럼 중 경기일자 컬럼명("game_date")은 실제 파일에 맞게 수정 필요
     */
    public List<Integer> getExpiredGames(){
        List<Integer> expired = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for(Map.Entry<String,Map<String,String>> entry : gameMap.entrySet()){
            String gnoStr = entry.getKey();
            String dateStr = entry.getValue().get("date");
            if(dateStr == null || dateStr.isEmpty())continue;

            try {
                LocalDate gameDate = LocalDate.parse(dateStr);
                if(gameDate.isBefore(today)){
                    expired.add(Integer.parseInt(gnoStr));
                }//if end
            } catch (Exception e) {
                System.out.println("날짜변환실패 gno ="+gnoStr + "date="+dateStr);
            }//catch end
        }//for end
        return expired;
    }//func end

}//class end