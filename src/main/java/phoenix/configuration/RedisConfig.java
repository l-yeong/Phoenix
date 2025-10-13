package phoenix.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * Redis 설정 클래스.
 * 기본 RedisTemplate은 JDK 직렬화를 사용하므로, 키/값을 문자열로 다룰 때
 * 직렬화 문제가 생기지 않도록 StringRedisSerializer를 설정한다.
 */
@Configuration
public class RedisConfig {

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

        // Redis에 저장되는 모든 value를 문자열로 직렬화 (예: "123456")
        tpl.setValueSerializer(stringSer);

        // Hash 구조의 key (Map의 key 역할) 도 문자열로 직렬화
        tpl.setHashKeySerializer(stringSer);

        // Hash 구조의 value (Map의 value 역할) 도 문자열로 직렬화
        tpl.setHashValueSerializer(stringSer);

        // 설정이 모두 끝난 뒤 초기화
        // → 내부 프로퍼티를 세팅한 후 Bean 등록 준비 완료
        tpl.afterPropertiesSet();


        // 최종적으로 완성된 RedisTemplate을 스프링 컨테이너에 Bean으로 등록
        // → 이후 다른 클래스(예: EmailService)에서 @Autowired 또는 생성자 주입으로 사용 가능
        return tpl;

    } // func e


} // class e

