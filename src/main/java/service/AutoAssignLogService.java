package service;

import lombok.RequiredArgsConstructor;
import model.mapper.AutoAssignLogMappder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoAssignLogService {
    private final AutoAssignLogMappder autoAssignLogMappder;

}//func end
