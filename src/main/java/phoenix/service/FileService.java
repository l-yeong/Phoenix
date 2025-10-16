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

}//class end