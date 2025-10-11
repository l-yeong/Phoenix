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

    @Select("SELECT t.ticket_code,t.price AS ticket_price,t.valid,r.status AS reservation_status, "+
            " m.mname,m.email,m.mphone,s.seat_no,z.zname,z.price AS zone_price " +
            " FROM tickets t JOIN reservations r ON t.rno = r.rno  JOIN members m ON r.mno = m.mno "+
            " JOIN seats s ON r.sno = s.sno JOIN zones z ON s.zno = z.zno WHERE m.mno = #{mno} ORDER BY t.tno ")
    public List<Map<String, Object>> ticketPrint(int mno);

}//inter end
