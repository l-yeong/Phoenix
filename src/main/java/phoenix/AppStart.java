package phoenix;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling //자동 티켓만료 필요한 스케줄링
public class AppStart {
    public static void main(String[] args) {
        SpringApplication.run(AppStart.class);
    }//main end
} // class end
//   이겨레 브랜치 커밋 테스트
// 251001_MembersMapper_회원가입 , 로그인 테스트용
// 251023_커밋확인용