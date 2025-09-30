package service;

import lombok.RequiredArgsConstructor;
import model.mapper.ZonesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ZonesService {
    private final ZonesMapper zonesMapper;

}//func end
