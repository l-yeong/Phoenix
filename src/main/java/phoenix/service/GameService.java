//package phoenix.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import phoenix.model.dto.GameDto;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class GameService {
//
//    private final CsvLoaderService csvLoaderService;
//
//    public List<GameDto> findAll() {
//        return csvLoaderService.loadGames();
//    }
//
//    public GameDto findByGno(int gno) {
//        return csvLoaderService.loadGames().stream()
//                .filter(g -> g.getGno() == gno)
//                .findFirst()
//                .orElse(null);
//    }
//
//    public boolean isReservable(int gno) {
//        GameDto g = findByGno(gno);
//        return g != null && g.isReservable();
//    }
//}