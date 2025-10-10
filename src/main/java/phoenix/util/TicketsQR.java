package phoenix.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.util.Map;

public class TicketsQR {

    // [*] qr 생성
    public static byte[] TicketQrCode(Map<String, Object> TicketQrMap) {
        ByteArrayOutputStream bout = null;
        try {
            // 1️⃣ QR에 넣을 문자열 만들기
            String qrData = TicketQrMap.toString(); // Map을 문자열로 변환 (원하는 형태로 변환 가능)

            // 2️⃣ BitMatrix 생성
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 200, 200);

            // 3️⃣ ByteArrayOutputStream 생성
            bout = new ByteArrayOutputStream();

            // 4️⃣ BitMatrix를 PNG로 변환
            MatrixToImageWriter.writeToStream(matrix, "PNG", bout);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bout.toByteArray();
    }

}//class end
