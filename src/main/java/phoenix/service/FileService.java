package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import phoenix.util.TicketsQR;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private final TicketsQR ticketsQR;
    private String baseDir = System.getProperty("user.dir");
    private String uploadPath = baseDir + "/build/resources/main/static/upload/";

    public String ImgQrSave(String text, int size){
        if(text==null || text.isEmpty()) return null;
        if(ticketsQR ==null){
            return null;
        }//if end

        try{
            Path dir = Paths.get(uploadPath);
            if(!Files.exists(dir)) Files.createDirectories(dir); // 지정경로가 없을시 새로운경로 생성

            String uuid = UUID.randomUUID().toString();
            String fileName = uuid + "_qr.png";
            Path output = Paths.get(uploadPath+fileName);

            byte[] png = ticketsQR.TicketQrCode(Map.of("text",text));
            Files.write(output,png);

            return fileName;
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }//catch end
    }//func end

}//class end
