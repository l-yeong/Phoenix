package model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MembersDto {
    private int mno;
    private String mname;
    private String mid;
    private String mphone;
    private String email;
    private String birthdate;
    private int pno;
    private String password_hash;
    private String create_at;
    private String provider;
    private String provider_id;
    private String status;
    private String exchange;

}//func end
