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
    public void init()  {

        try{
            FileInputStream serviceAccount =
                    new FileInputStream("src/main/resources/static/my-test-project-9ae49-firebase-adminsdk-fbsvc-7937c5b3cc.json");

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);

        } catch (Exception e) {
            System.out.println("FireBaseConfig:"+ e);
        }//catch end

    }//func end
}//class end
