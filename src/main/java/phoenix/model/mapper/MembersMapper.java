package phoenix.model.mapper;

import phoenix.model.dto.MembersDto;
import org.apache.ibatis.annotations.*;

/*
* Mybatis Mapper
* - DB 연동
* - 회원정보 저장 및 조회
* */
@Mapper
public interface MembersMapper {

//    // 회원가입
//    @Insert("INSERT INTO members(mid, password_hash, mname, mphone, birthdate, email) " +
//            "VALUES(#{mid}, #{password_hash}, #{mname}, #{mphone}, #{birthdate}, #{email})")
//    int signUp(MembersDto member);
//
//    // 로그인
//    @Select("SELECT * FROM members WHERE mid = #{mid} AND password_hash = #{password_hash} AND status = 'active'")
//    MembersDto login(@Param("mid") String mid, @Param("password_hash") String password_hash);
//
//    // 아이디 중복 체크
//    @Select("SELECT COUNT(*) FROM members WHERE mid = #{mid}")
//    int checkDuplicateId(String mid);

    /**
    *   회원가입 메소드
    *   @param membersDto 회원정보 DTO
    *   @return int ( 1 : 성공 ,  0 : 실패 )
    */
    @Insert("""
        insert into members 
        (mid, password_hash, mname, mphone, birthdate, email, provider, provider_id, pno, status, exchange, email_verified)
        values 
        (#{mid}, #{password_hash}, #{mname}, #{mphone}, #{birthdate}, #{email}, #{provider}, #{provider_id}, #{pno}, #{status}, #{exchange}, #{email_verified})
    """)
    int signUp( MembersDto membersDto );

    /**
     *  아이디로 회원 조회 메소드
     * @param mid
     * @return MembersDto
     */
    @Select("select * from members where mid = #{mid}")
    MembersDto findByMid(String mid);

    /**
     *  리프레시 토큰 저장(토큰 재발급용)
     * @param membersDto
     * @return int
     */
    @Update("update members set refresh_token = #{refresh_token} , refresh_token_expire = #{refresh_token_expire}" +
            "where mno = #{mno}")
    int saveRefreshToken(MembersDto membersDto);

    /**
     *  이메일 인증 완료 업데이트
     * @param email
     * @return int
     * */
    @Update("update members set email_verified = true where email = #{email}")
    int verifyEmail(String email);




} // class e
