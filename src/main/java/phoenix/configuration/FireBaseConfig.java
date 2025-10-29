package phoenix.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration

// Firebase Cloud Messaging 서버와 통신
public class FireBaseConfig {
    @PostConstruct // 스프링이 이클래스를 로딩한 직후 자동으로 실행
    public void init() throws IOException {
        // 이미 FirebaseApp 이 초기화 되어 있는지 확인(중복 초기화 방지)
        if (FirebaseApp.getApps().isEmpty()) {
            //Firebase 서비스 계정 키(JSON) 파일을 읽음
            // 실제 경로는 서버 환경에 맞게 수정 해야함
            try (var in = new FileInputStream("/path/serviceAccountKey.json")) {

                // Firebase 옵션 구성(자격증명만 설정)
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))// 서비스 계정 키 기반 인증
                        .build();

                //Firebase SDK 초기화(FirebaseMessaging 사용가능)
            }//try end
        }//if end
    }//func end
}//class end
