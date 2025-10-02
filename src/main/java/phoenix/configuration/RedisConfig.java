package phoenix.configuration;// 패키지명

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {// class start
    @Value("${spring.redis.host}")
    private String host;
    @Value("${spring.redis.port}")
    private int port;
    @Value("${spring.redis.password}")
    private String password;

    /**
     * Redis 연결 Factory 빈 등록
     *
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);       // Redis 서버 주소
        config.setPort(port);           // Redis 서버 포트
        config.setPassword(password);   // Redis 비밀번호
        return new LettuceConnectionFactory(config);
    }// func end

    /**
     * Redis 데이터 처리 템플릿 설정
     *
     * @return RedisTemplate<String,Object>
     */
    @Bean
    public RedisTemplate<String,Object> redisTemplate(){
        // 템플릿 생성
        RedisTemplate<String,Object> template = new RedisTemplate<>();
        // 템플릿에 redis 서버 연결
        template.setConnectionFactory(redisConnectionFactory());
        // Hash Key,value 는 value를 Map 처럼 사용할때 사용되는 key value
        // key , HashKey 타입을 String 으로 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        // value , HashValue 타입을 Object 으로 설정(JSON)
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        return template;
    }// func end


}// class end
