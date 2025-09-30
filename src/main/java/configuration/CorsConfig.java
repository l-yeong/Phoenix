package configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                    // 허용할 컨트롤러 URL
                .allowedOrigins("http://localhost:5173")        // 허용할 도메인
                .allowedMethods("GET","POST","PUT","DELETE")    // 허용할 메소드
                .allowCredentials(true)                         // 세션유지 허용
                .allowedHeaders("*");                           // HTTP 헤더 정보 허용
    }
}
