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
     *  이메일 인증 완료 업데이트
     * @param email
     * @return int
     * */
    @Update("update members set email_verified = true where email = #{email}")
    int verifyEmail(String email);

    /**
     *  이메일로 회원 조회
     *  @param email
     *  @return MembersDto
     */
    @Select("select * from members where email = #{email}")
    MembersDto findByEmail(String email);



} // class e
