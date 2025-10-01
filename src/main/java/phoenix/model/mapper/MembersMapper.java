package phoenix.model.mapper;

import phoenix.model.dto.MembersDto;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MembersMapper {

    // 회원가입
    @Insert("INSERT INTO members(mid, password_hash, mname, mphone, birthdate, email) " +
            "VALUES(#{mid}, #{password_hash}, #{mname}, #{mphone}, #{birthdate}, #{email})")
    int signUp(MembersDto member);

    // 로그인
    @Select("SELECT * FROM members WHERE mid = #{mid} AND password_hash = #{password_hash} AND status = 'active'")
    MembersDto login(@Param("mid") String mid, @Param("password_hash") String password_hash);

    // 아이디 중복 체크
    @Select("SELECT COUNT(*) FROM members WHERE mid = #{mid}")
    int checkDuplicateId(String mid);



}//inter end
