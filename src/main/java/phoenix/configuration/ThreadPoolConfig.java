package phoenix.configuration; // 패키지명

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


import java.util.concurrent.Executor;


@Configuration
public class ThreadPoolConfig { // class start

    /**
     * 쓰레드풀 설정값 (교환신청)
     *
     * @return executor
     */
    @Bean
    public Executor changeExecutor(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                // 기본 쓰레드수 2
        executor.setMaxPoolSize(5);                 // 최대 쓰레드수 5
        executor.setQueueCapacity(5);               // 큐 용량(대기열)
        executor.setThreadNamePrefix("change-");    // 스레드 이름 앞부분
        executor.initialize();                      // 생성
        return executor;
    }// func end
}// class end
