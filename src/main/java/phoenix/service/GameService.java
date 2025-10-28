package phoenix.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import phoenix.model.dto.GameDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    } // func e

    public boolean isReservable(int gno) {
        GameDto g = findByGno(gno);
        return g != null && g.isReservable();
    } // func e


    /**
     * 오늘 경기 포함 , 아직 시작하지 않은 경기 3개 반환
     */
    public List<GameDto> findUpcomingGames(){
        LocalDateTime now = LocalDateTime.now();

        return fileService.loadGames().stream()
                .filter(g->{
                    LocalDate date = g.getDate();
                    LocalTime time = g.getTime();

                    // date/time 둘 중 하나라도 null 이면 그냥 통과
                    if( date == null || time == null ) return false;

                    LocalDateTime gameDateTime = LocalDateTime.of(date , time);
                    return !gameDateTime.isBefore(now);
                })
                .sorted(Comparator
                        .comparing(GameDto::getDate)
                        .thenComparing(GameDto::getTime))
                .limit(3)
                .collect(Collectors.toList());

    } // func e


} //class e
