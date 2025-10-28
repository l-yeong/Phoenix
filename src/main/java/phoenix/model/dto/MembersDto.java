package phoenix.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "이름은 2~20자 이내여야 합니다.")
    private String mname;

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,12}$", message = "아이디는 영문과 숫자로 4~12자 이내여야 합니다.")
    private String mid;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-0000-0000 형식으로 입력해주세요.")
    private String mphone;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함한 8~20자여야 합니다."
    )
    private String password_hash;

    @NotBlank(message = "생년월일은 필수 입력 항목입니다.")
    private String birthdate;
    private int pno;                // 선호선수(csv/크롤링 매칭)
    private String create_at;       // 가입일
    private String provider;        // 소셜제공자
    private String provider_id;     // 소설제공자내부고유ID
    private String status;          // 회원상태
    private Boolean exchange;        // 교환 신청 가능
    private Boolean email_verified; // 이메일 인증 여부
    private LocalDateTime last_status_change; // 마지막 상태변경 날짜

}//func end
