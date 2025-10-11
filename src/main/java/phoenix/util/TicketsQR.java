package phoenix.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class TicketsQR {

    // [*] qr 생성
    public static byte[] TicketQrCode(Map<String, Object> TicketQrMap) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            // Map 에서 JOSN 문자열 (한글)
            // BitMatrix 생성
            ObjectMapper mapper = new ObjectMapper();
            String qrData = mapper.writeValueAsString(TicketQrMap);

            // QR UTF-8 설정
            Map<EncodeHintType, Object>hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET,"UTF-8"); // QR 코드 문자 인코딩
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L); // QR 이미지 일부 가려져있을때 읽을수 있는 복원 레벨

            // QR 생성
            BitMatrix matrix = new MultiFormatWriter().encode(qrData,BarcodeFormat.QR_CODE,200, 200, hints);

            // PNG로 변환
            MatrixToImageWriter.writeToStream(matrix, "PNG", bout);

            return bout.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
        }//catch end
        return new byte[0];
    }//func end

}//class end
