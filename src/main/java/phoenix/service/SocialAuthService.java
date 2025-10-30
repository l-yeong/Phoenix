package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import phoenix.model.dto.MembersDto;
import phoenix.model.mapper.MembersMapper;
import phoenix.security.JwtUtil;
import phoenix.util.SocialUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * SocialAuthService
 * <p>Google, GitHub, Facebook 등 소셜 로그인 처리 및 JWT 발급 담당</p>
 * <p>1. OAuth2 인증 후 회원 조회 및 신규 생성</p>
 * <p>2. JWT Access / Refresh 토큰 발급 및 Redis 저장</p>
 */
@Service
@RequiredArgsConstructor
public class SocialAuthService {

    private final MembersMapper membersMapper;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final SocialUtil socialUtil;

    /**
     * 소셜 로그인 처리
     *
     * @param provider   소셜 제공자 (google, github, facebook 등)
     * @param providerId 소셜 플랫폼의 사용자 고유 ID
     * @return Access Token (로그인 성공 시) 또는 null (신규 회원)
     */
    @Transactional(readOnly = true)
    public String socialLogin(String provider, String providerId) {
        MembersDto member = membersMapper.findByProvider(provider, providerId);
        if (member == null) {
            // 신규 회원 → 프론트에서 추가정보 입력 유도
            return null;
        }


        // 세션 기반 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                member, // Principal: 로그인한 회원 정보
                null,   // Credentials
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);


        // JWT 토큰 생성
        String accessToken = jwtUtil.generateToken(member);
        String refreshToken = jwtUtil.generateRefreshToken(member.getMid());

        // Redis 저장 (7일 TTL)
        tokenService.saveRefreshToken(member.getMid(), refreshToken, 7 * 24 * 60);

        return accessToken;
    } // func e

    /**
     * 소셜 신규 회원가입
     * <p>OAuth 인증 완료 후 추가 정보 입력 시 호출됨</p>
     * <p>※ Google/Facebook 등에서 제공하지 않는 필드는 기본값으로 채움</p>
     *
     * @param membersDto 신규 회원 정보 DTO
     * @return true (성공) / false (실패)
     */
    @Transactional
    public boolean socialSignUp(MembersDto membersDto) {

        // [1] null 방지 기본값 세팅
        if (membersDto.getBirthdate() == null) {
            membersDto.setBirthdate(String.valueOf(LocalDate.of(1900, 1, 1))); // 기본 생년월일
        }
        if (membersDto.getMphone() == null || membersDto.getMphone().isBlank()) {
            membersDto.setMphone(SocialUtil.generateTempPhone(membersDto.getProvider_id()));
        }

        if (membersDto.getMname() == null || membersDto.getMname().isBlank()) {
            membersDto.setMname("social_user_" + membersDto.getProvider_id());
        }
        if (membersDto.getEmail() == null || membersDto.getEmail().isBlank()) {
            membersDto.setEmail(membersDto.getProvider() + "_" + membersDto.getProvider_id() + "@social.local");
        }

        // 이메일 비어있으면 provider 기반으로 임시 생성
        if(membersDto.getEmail() == null || membersDto.getEmail().isBlank()){
            String tempEmail = membersDto.getProvider() + "_" + membersDto.getProvider_id() + "@@social.local";
            membersDto.setEmail(tempEmail);
        }

        // mid 생성 (null 방지)
        if(membersDto.getMid() == null || membersDto.getMid().isBlank()){
            String midValue = (membersDto.getEmail() != null && !membersDto.getEmail().isBlank())
                    ? membersDto.getEmail()
                    : "social_" + membersDto.getProvider() + "_" + membersDto.getProvider_id();
            membersDto.setMid(midValue);
        }


        // [2] 필수 초기값 설정
        membersDto.setStatus("active");
        membersDto.setExchange(true);
        membersDto.setEmail_verified(true); // 소셜 로그인은 이메일 검증 완료 상태로 간주

        // [3] 중복 검사 (email, provider_id)
        MembersDto existingByEmail = membersMapper.findByEmail(membersDto.getEmail());
        MembersDto existingByProvider = membersMapper.findByProvider(
                membersDto.getProvider(), membersDto.getProvider_id()
        );

        if (existingByEmail != null || existingByProvider != null) {
            return false; // 중복 방지
        }

        // [4] DB 저장
        return membersMapper.signUp(membersDto) > 0;
    } // func e


    @Transactional
    public MembersDto findMemberByProvider(String provider, String providerId) {
        return membersMapper.findByProvider(provider, providerId);
    } // func e

} // class e
