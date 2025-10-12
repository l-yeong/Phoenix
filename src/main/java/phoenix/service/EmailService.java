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

    // JavaMailSender : 스프링에서 제공하는 이메일 전송 기능 객체
    private final JavaMailSender mailSender;

    // RedisTemplate : Redis 서버와 데이터를 주고받기 위한 객체 (String-String 타입)
    private final RedisTemplate< String , String > redisTemplate;

    /**
     * 인증코드 생성 및 발송
     *
     * @param email 수신자 이메일
     * @return 인증코드
     */
    public String sendAuthCode(String email){

        // [1] 6자리 난수 생성
        // Math.random() * 900000 → 0~899999 범위 생성
        // + 100000 → 최소 100000 ~ 최대 999999 (즉, 6자리 보장)
        String code = String.valueOf((int)(Math.random() * 900000) + 100000);

        // [2] Redis에 인증코드 저장
        // key : 이메일 주소
        // value : 인증코드
        // 5분(300초) 동안만 유효하도록 TTL 설정
        redisTemplate.opsForValue().set(email , code , 5 , TimeUnit.MINUTES); // 5분 유효

        // [3] 메일 객체 생성
        // SimpleMailMessage : 간단한 텍스트 메일 전송용 클래스
        SimpleMailMessage message = new SimpleMailMessage();

        // [4] 수신자 이메일 주소 지정
        message.setTo(email);

        // [5] 메일 제목 지정
        message.setSubject("[Phoenix] 이메일 코드");

        // [6] 메일 본문 내용 작성
        message.setText("인증코드 : " + code + "\n 5분 이내에 입력해주세요.");

        // [7] 메일 발송
        mailSender.send(message);

        // [8] 생성된 인증코드를 반환 (테스트나 로그용)
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

        // [1] Redis에서 해당 이메일(key)에 저장된 인증코드를 가져옴
        String savedCode = redisTemplate.opsForValue().get(email);

        // [2] 저장된 코드가 존재하고, 입력값과 일치할 경우
        if(savedCode != null && savedCode.equals(code)){

            // [3] 인증이 성공했으므로 Redis에서 해당 key(이메일) 삭제
            // → 보안상, 한 번 사용된 코드는 재사용 방지
            redisTemplate.delete(email);

            // [4] true 반환 → 인증 성공
            return true;
        } // if e

        // [5] 코드가 없거나 불일치 → 인증 실패
        return false;
    } // func e


    /**
     * 일반 메일 발송 (임시 비밀번호, 공지 등)
     *
     * @param to 수신자 이메일
     * @param subject 제목
     * @param text 본문 내용
     */
    public void sendSimpleMail(String to, String subject, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.send(msg);
    } // func e


} // class e
