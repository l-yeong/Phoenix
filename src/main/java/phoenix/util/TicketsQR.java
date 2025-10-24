package phoenix.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
@Component
public class TicketsQR {

    // [*] qr 생성

    public static byte[] TicketQrCode(String url, int size) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

            // text 문자열 그대로 QR 생성
            BitMatrix matrix = new MultiFormatWriter().encode(
                    url, BarcodeFormat.QR_CODE, size, size, hints);

            MatrixToImageWriter.writeToStream(matrix, "PNG", bout);
            return bout.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("QR 생성 실패", e);
        }
    }

}//class end
