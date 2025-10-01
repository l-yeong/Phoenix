package service;

import lombok.RequiredArgsConstructor;
import model.mapper.RolesMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RolesService {
    private final RolesMapper rolesMapper;

}//func end
