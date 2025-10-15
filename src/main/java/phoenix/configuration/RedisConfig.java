package phoenix.configuration;// 패키지명

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
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

    /**
     * 분산 lock 을 위한 설정
     *
     * @return RedissonClient
     */
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        // 단일 redis 서버 설정
        config.useSingleServer().setAddress("redis://"+host+":"+port);
        // RedissonClient 생성
        return Redisson.create(config);
    }// func end

    /**
     * 메소드명: redisTemplate
     * 설명: 키/값/해시 모두 String 직렬화를 적용한 RedisTemplate 빈을 제공한다.
     *
     * @param connectionFactory RedisConnectionFactory (스프링이 자동 주입)
     * @return String-String 타입의 RedisTemplate 빈
     */
    @Bean
    public RedisTemplate<String , Object> redisTemplate(RedisConnectionFactory connectionFactory){

        // RedisTemplate 객체 생성
        // → Redis 서버와 데이터를 주고받을 수 있는 핵심 도구
        RedisTemplate<String , Object> tpl = new RedisTemplate<>();

        // Redis 서버와의 실제 연결(커넥션)을 담당하는 객체 주입
        // → Spring Boot가 자동으로 RedisConnectionFactory를 구성해준다.
        tpl.setConnectionFactory(connectionFactory);

        // StringRedisSerializer 생성
        // → Redis에 데이터를 "문자열" 형태로 저장/조회할 수 있도록 직렬화 도구를 지정
        StringRedisSerializer stringSer = new StringRedisSerializer();
        // Redis에 저장되는 모든 key를 문자열로 직렬화 (예: "hong@test.com")
        tpl.setKeySerializer(stringSer);
        // Hash 구조의 key (Map의 key 역할) 도 문자열로 직렬화
        tpl.setHashKeySerializer(stringSer);

        Jackson2JsonRedisSerializer<Object> jsonSer = new Jackson2JsonRedisSerializer<>(Object.class);
        // Redis에 저장되는 모든 value를 object로 직렬화
        tpl.setValueSerializer(jsonSer);
        // Hash 구조의 value (Map의 value 역할) 도 object로 직렬화
        tpl.setHashValueSerializer(jsonSer);

        // 설정이 모두 끝난 뒤 초기화
        // → 내부 프로퍼티를 세팅한 후 Bean 등록 준비 완료
        tpl.afterPropertiesSet();


        // 최종적으로 완성된 RedisTemplate을 스프링 컨테이너에 Bean으로 등록
        // → 이후 다른 클래스(예: EmailService)에서 @Autowired 또는 생성자 주입으로 사용 가능
        return tpl;

    } // func e


}// class end
