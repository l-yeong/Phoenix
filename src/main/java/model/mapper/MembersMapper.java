package model.mapper;

import model.dto.MembersDto;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MembersMapper {

    // 회원가입 (일반회원만 - mid, password_hash, 이름, 전화번호, 생년월일, 이메일)
    @Insert("insert into members(mid, password_hash, mname, mphone, birthdate, email) " +
            "VALUES(#{mid}, #{password_hash}, #{mname}, #{mphone}, #{birthdate}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "mno")
    int signUp(MembersDto member);

    // 로그인 (아이디 + 비밀번호 + 활성 회원 상태)
    @Select("SELECT * FROM members WHERE mid = #{mid} AND password_hash = #{password_hash} AND status = 'active'")
    MembersDto login(@Param("mid") String mid, @Param("password_hash") String password_hash);


}//inter end
