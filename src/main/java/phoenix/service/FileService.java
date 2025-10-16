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
    private String uploadPath = baseDir + "/src/main/resources/static/upload/"; //QR이미지 저장 경로

    public String ImgQrSave(String text, int size){
        if(text==null || text.isEmpty()) return null;
        if(ticketsQR ==null){
            return null;
        }//if end

        try{
            Path dir = Paths.get(uploadPath); // 업로드 폴더 경로 준비
            if(!Files.exists(dir)) Files.createDirectories(dir); // 지정경로가 없을시 새로운경로 생성

            String uuid = UUID.randomUUID().toString(); //UUID 생성
            String fileName = uuid + "_qr.png";

            // 저장될 파일의 전체 경로
            Path output = Paths.get(uploadPath+fileName);

            // QR 이미지 생성
            byte[] png = ticketsQR.TicketQrCode(Map.of("text",text));
            Files.write(output,png);

            return fileName;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }//catch end
    }//func end

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
            CSVReader reader = new CSVReader(new FileReader("games.csv"));
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