package phoenix.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import phoenix.model.dto.MembersDto;
import phoenix.service.MembersService;
import phoenix.util.ApiResponseUtil;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/senior")
@RequiredArgsConstructor
public class SeniorController {

    private final MembersService membersService;


    /**
     * 시니어 회원
     */

    @GetMapping("/reserve")
    public ResponseEntity<?> seniorReserve() {

        MembersDto member = membersService.getLoginMember();
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseUtil<>(false, "로그인이 필요합니다.", null));
        }

        if(member.getBirthdate() == null || member.getBirthdate().isBlank()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseUtil<>( false , "생년월일 정보가 없습니다." , null));
        }

        LocalDate birthDate;

        try {
            birthDate = LocalDate.parse(member.getBirthdate()); // String -> LocalDate 반환

        }catch (DateTimeParseException e ){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponseUtil<>( false , "생년월일 형식이 올바르지 않습니다." , null));
        }

        int age = Period.between(birthDate , LocalDate.now()).getYears();
        if (age < 65) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponseUtil<>(false, "시니어 전용 서비스입니다.", null));
        }

        return ResponseEntity.ok(new ApiResponseUtil<>(true, "접근 허용", null));

    } // func e


} // class e
