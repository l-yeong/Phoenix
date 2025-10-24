package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/*
* 회원 정보 담는 DTO 클래스
* */

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MembersDto {
    private int mno;                // 회원번호
    private String mname;           // 회원명
    private String mid;             // 회원아이디
    private String mphone;          // 회원전화번호
    private String email;           // 회원이메일
    private String birthdate;       // 회원생년월일
    private int pno;                // 선호선수(csv/크롤링 매칭)
    private String password_hash;   // 비밀번호 해시값
    private String create_at;       // 가입일
    private String provider;        // 소셜제공자
    private String provider_id;     // 소설제공자내부고유ID
    private String status;          // 회원상태
    private Boolean exchange;        // 교환 신청 가능
    private Boolean email_verified; // 이메일 인증 여부
    private LocalDateTime last_status_change; // 마지막 상태변경 날짜

}//func end
