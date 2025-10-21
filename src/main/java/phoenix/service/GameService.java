package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phoenix.model.dto.GameDto;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final FileService fileService;

    public List<GameDto> findAll() {
        return fileService.loadGames();
    }

    public GameDto findByGno(int gno) {
        return fileService.loadGames().stream()
                .filter(g -> g.getGno() == gno)
                .findFirst()
                .orElse(null);
    }

    public boolean isReservable(int gno) {
        GameDto g = findByGno(gno);
        return g != null && g.isReservable();
    }
}