package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.ZonesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZonesService {
    private final ZonesMapper zonesMapper;

}//func end
