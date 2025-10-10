package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;
import phoenix.util.TicketsQR;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;

    public List<byte[]> ticketPrint(int mno){
        List<Map<String, Object>> result = ticketsMapper.ticketPrint(mno);

        return result.stream()
                .map(ticketMap -> TicketsQR.TicketQrCode(ticketMap)) //static 호출
                .toList();
    }//func end

}//func end
