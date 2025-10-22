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

    /** 소셜 계정으로 회원 조회 */
    @Select("SELECT * FROM members WHERE provider = #{provider} AND provider_id = #{provider_id}")
    MembersDto findByProvider(@Param("provider") String provider, @Param("provider_id") String provider_id);

    /**
     * 회원 정보 수정
     * */
    @Update("""
        UPDATE members
        SET mname = #{dto.mname},
            mphone = #{dto.mphone},
            email = #{dto.email},
            email_verified = #{dto.email_verified},
            pno = #{dto.pno},
            exchange = #{dto.exchange}
        WHERE mid = #{mid}
    """)
    int infoUpdate(@Param("mid") String mid, @Param("dto") MembersDto dto);

    /**
     * 비밀번호 변경
     * */
    @Update("UPDATE members SET password_hash = #{newPwd} WHERE mid = #{mid}")
    int pwdUpdate(@Param("mid") String mid, @Param("newPwd") String newPwd);

    /**
     * 회원 탈퇴
     * */
    @Update("UPDATE members SET status = 'withdrawn' , email = #{newEmail} WHERE mid = #{mid}")
    int memberDelete(String mid , String newEmail );

    /**
     * 아이디 찾기
     * */
    @Select("SELECT mid FROM members WHERE email = #{email} AND status != 'withdrawn'")
    String findId(String email);

    /** 이름 + 연락처 + 이메일로 회원 조회 (아이디 찾기용) */
    @Select("""
        SELECT * FROM members 
        WHERE mname = #{mname} AND mphone = #{mphone} AND email = #{email}
          AND status != 'withdrawn'
    """)
    MembersDto findByNamePhoneEmail(@Param("mname") String mname,
                                    @Param("mphone") String mphone,
                                    @Param("email") String email);

    /** 아이디 + 이름 + 이메일로 회원 조회 (비밀번호 재설정용) */
    @Select("""
        SELECT * FROM members 
        WHERE mid = #{mid} AND mname = #{mname} AND email = #{email}
          AND status != 'withdrawn'
    """)
    MembersDto findByMidNameEmail(@Param("mid") String mid,
                                  @Param("mname") String mname,
                                  @Param("email") String email);


} // class e
