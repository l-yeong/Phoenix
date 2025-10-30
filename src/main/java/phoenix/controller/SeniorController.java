package phoenix.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import phoenix.model.dto.AutoSelectDto;
import phoenix.model.dto.GameDto;
import phoenix.model.dto.MembersDto;
import phoenix.service.GameService;
import phoenix.service.MembersService;
import phoenix.util.ApiResponseUtil;
import phoenix.service.SeniorReservationService;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/senior")
@RequiredArgsConstructor
public class SeniorController {

    private final MembersService membersService;
    private final GameService gameService;
    private final SeniorReservationService seniorService;


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

    /**
     * 오늘 경기 포함 , 아직 시작하지 않은 경기 3개 반환
     */
    @GetMapping("/games")
    public ResponseEntity<ApiResponseUtil<?>> getUpcomingGames(){
        List<GameDto> games = gameService.findUpcomingGames();
        return ResponseEntity.ok(new ApiResponseUtil<>(true , "예매 가능한 경기" , games));

    } // func e

    /**
     * 시니어 예매 두번째 페이지 경기제목 가져오기
     */
    @GetMapping("/games{gno}")
    public ResponseEntity<ApiResponseUtil<?>> getGame(@PathVariable int gno){
        GameDto game = gameService.findByGno(gno);

        if(game == null){
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseUtil<>(false , "경기 정보를 찾을 수 없습니다." , null));
        }

        return ResponseEntity.ok(new ApiResponseUtil<>(true , "경기 조회 성공" , game));
    }

    /**
     * 🧓 시니어 자동예매 (즉시 확정)
     * - DTO는 일반 자동예매와 동일(재사용)
     * - ok=true면 이미 DB/Redis 반영 완료 상태
     */
    @PostMapping("/auto")
    public ResponseEntity<AutoSelectDto.AutoSelectRes> auto(@RequestBody AutoSelectDto.AutoSelectReq req) {
        int mno = membersService.getLoginMember().getMno();
        AutoSelectDto.AutoSelectRes res = seniorService.autoBookAndConfirm(mno, req);
        return ResponseEntity.ok(res);
    }


} // class e