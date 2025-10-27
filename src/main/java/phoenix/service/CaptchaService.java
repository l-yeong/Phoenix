package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.redisson.api.RBucket;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedissonClient redisson;
    private static final SecureRandom RND = new SecureRandom();
    private static final String REDIS_PREFIX = "captcha:";
    private static final Duration TTL = Duration.ofMinutes(2); // 2분 유효
    private static final int WIDTH = 180, HEIGHT = 60;

    public String randomCode(int len) {
        // 헷갈리는 문자 제거(0/O, 1/I/L 등)
        String alphabet = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(alphabet.charAt(RND.nextInt(alphabet.length())));
        return sb.toString();
    }

    public String hash(String s) {
        // 간단히 소문자 트림 + 해시 (실서비스: HMAC-SHA256 추천)
        return Integer.toHexString(s.toLowerCase().trim().hashCode());
    }

    public String renderPngBase64(String code) throws Exception {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // 배경
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 노이즈 라인
        for (int i = 0; i < 10; i++) {
            g.setColor(new Color(RND.nextInt(255), RND.nextInt(255), RND.nextInt(255), 120));
            int x1 = RND.nextInt(WIDTH), y1 = RND.nextInt(HEIGHT);
            int x2 = RND.nextInt(WIDTH), y2 = RND.nextInt(HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 문자 그리기(약간의 왜곡/회전)
        g.setFont(new Font("Arial", Font.BOLD, 36));
        FontMetrics fm = g.getFontMetrics();
        int totalW = fm.stringWidth(code);
        int x = (WIDTH - totalW) / 2, y = (HEIGHT + fm.getAscent()) / 2 - 8;

        for (char ch : code.toCharArray()) {
            String s = String.valueOf(ch);
            int w = fm.charWidth(ch);
            AffineTransform old = g.getTransform();
            double angle = (RND.nextDouble() - 0.5) * 0.5; // -0.25~0.25rad
            g.rotate(angle, x + w/2.0, y - fm.getAscent()/2.0);
            g.setColor(new Color(20+RND.nextInt(80), 20+RND.nextInt(80), 20+RND.nextInt(80)));
            g.drawString(s, x, y);
            g.setTransform(old);
            x += w + 2;
        }

        // 점 노이즈
        for (int i = 0; i < 300; i++) {
            img.setRGB(RND.nextInt(WIDTH), RND.nextInt(HEIGHT),
                    new Color(RND.nextInt(255), RND.nextInt(255), RND.nextInt(255)).getRGB());
        }

        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public CaptchaPair newCaptcha() throws Exception {
        String code = randomCode(6);
        String token = UUID.randomUUID().toString();
        String hash = hash(code);
        // Redis 저장 (해시만 저장)
        RBucket<String> b = redisson.getBucket(REDIS_PREFIX + token);
        b.set(hash, TTL);
        String img = renderPngBase64(code);
        return new CaptchaPair(token, img);
    }

    public int verify(String token, String answer) {
        RBucket<String> b = redisson.getBucket(REDIS_PREFIX + token);
        String saved = b.get();
        if (saved == null) return -1;
        boolean ok = saved.equals(hash(answer));
        // 단발성 사용: 성공/실패 상관없이 삭제 권장(재사용 방지)
        b.delete();
        if(ok) return 1;
        else return 0;
    }

    public record CaptchaPair(String token, String imageBase64) {}
}
