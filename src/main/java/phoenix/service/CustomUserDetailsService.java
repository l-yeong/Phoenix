package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;


/**
 *   사용자 인증 정보 로드 서비스
 * - Spring Security의 UserDetailsService 인터페이스를 구현
 * - 로그인 시 Security가 자동으로 호출해서 DB에서 사용자 정보를 조회함
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MembersMapper membersMapper;


    /**
     * - 로그인 시 호출되는 메서드 (아이디를 기준으로 DB 조회)
     * - 사용자 정보를 UserDetails 형태로 반환해야 Security가 인증 절차를 수행함
     */
    @Override
    public UserDetails loadUserByUsername(String mid) throws UsernameNotFoundException {
        MembersDto member = membersMapper.findByMid(mid);
        if( member == null ){
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다." + mid);
        }

        return User.builder()
                .username(member.getMid()) // 로그인 아이디
                .password(member.getPassword_hash()) // 암호화된 비밀번호
                .roles("USER")              // 기본 권한 (role 컬럼이 없으므로 임시로 USER 고정)
                .build();

    } // func e

} // class e

