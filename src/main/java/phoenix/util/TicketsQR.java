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

    /**
     * QR 코드 이미지를 생성.
     *
     * @param url   QR 코드에 포함될 문자열 (예: URL 또는 텍스트)
     * @param size  QR 코드 이미지의 가로·세로 크기 (픽셀 단위)
     * @return      PNG 형식의 QR 코드 이미지 바이트 배열
     * @throws RuntimeException QR 코드 생성 실패 시 발생
     */
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
        }//catch end
    }//func end

}//class end
