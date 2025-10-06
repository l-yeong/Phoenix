package phoenix.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 이메일 인증 서비스
 * - 인증코드 생성, Redis 저장, 메일 발송, 검증 처리
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate< String , String > redisTemplate;

    /**
     * 인증코드 생성 및 발송
     *
     * @param email 수신자 이메일
     * @return 인증코드
     */
    public String sendAuthCode(String email){
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);
        redisTemplate.opsForValue().set(email , code , 5 , TimeUnit.MINUTES); // 5분 유효

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Phoenix] 이메일 코드");
        message.setText("인증코드 : " + code + "\n 5분 이내에 입력해주세요.");
        mailSender.send(message);

        return code;

    } // func e


    /**
     * 인증코드 검증
     *
     * @param email 이메일 주소
     * @param code 입력한 코드
     * @return true(인증 성공), false(실패)
     */
    public boolean verifyCode( String email , String code ){
        String savedCode = redisTemplate.opsForValue().get(email);
        if(savedCode != null && savedCode.equals(code)){
            redisTemplate.delete(email);
            return true;
        } // if e
        return false;
    } // func e


} // class e
