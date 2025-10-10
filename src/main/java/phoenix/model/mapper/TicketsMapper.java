package phoenix.model.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import phoenix.model.dto.TicketsDto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Mapper
public interface TicketsMapper {

    @Select("select t.tno,t.ticket_code,t.price as ticket_price,t.valid,r.rno,r.status " +
    " as reservation_status,m.mno,m.mname,m.email,m.mphone,s.sno,s.seat_no,z.zname,z.price " +
    " as zone_price from tickets t join reservations r on t.rno = r.rno join members m on r.mno = m.mno " +
    " join seats s on r.sno = s.sno join zones z on s.zno = z.zno where m.mno=#{mno} order by t.tno ;")
    public List<Map<String, Object>> ticketPrint(int mno);

}//inter end
