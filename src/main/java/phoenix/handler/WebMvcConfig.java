package phoenix.handler;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // FileService.uploadPath와 반드시 동일해야 합니다.
        String absPath = System.getProperty("user.dir") + "/src/main/resources/static/upload/";
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + absPath); // "file:" prefix 필수
    }
}