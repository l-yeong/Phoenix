package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.TicketsMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketsService {
    private final TicketsMapper ticketsMapper;

}//func end
