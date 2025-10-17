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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final TicketsQR ticketsQR;
    private Map<String , Map<String ,String >> gameMap;


    private String baseDir = System.getProperty("user.dir"); //루트 디렉터리 경로
    private String uploadPath = baseDir + "/src/main/resources/static/upload/"; //QR 이미지 저장 경로

    public String saveQRImg(String payload) {
        try {
            Path dir = Paths.get(uploadPath);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String fileName = UUID.randomUUID() + "_qr.png";
            Path output = dir.resolve(fileName);

            byte[] png = ticketsQR.TicketQrCode(Map.of("text",payload)); // size 옵션 지원시
            Files.write(output, png);

            return "/upload/" + fileName; // ★ 정적 경로(URL) 반환
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

}//class end