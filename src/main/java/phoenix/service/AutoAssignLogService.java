package phoenix.service;

import lombok.RequiredArgsConstructor;
import phoenix.model.mapper.AutoAssignLogMappder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoAssignLogService {
    private final AutoAssignLogMappder autoAssignLogMappder;

}//func end
