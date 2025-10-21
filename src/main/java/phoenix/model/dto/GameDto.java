package phoenix.model.dto;

import lombok.*;
import java.time.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameDto {
    private int gno;
    private String homeTeam;
    private String homePitcher;
    private String awayTeam;
    private String awayPitcher;
    private LocalDate date;
    private LocalTime time;
    private String result;
    private String score;

    // ✅ 예매 가능 여부 계산
    public boolean isReservable() {
        LocalDateTime gameDateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        // 7일 전부터 경기 시작 전까지 예매 가능
        boolean withinPeriod = now.isAfter(gameDateTime.minusDays(7)) && now.isBefore(gameDateTime);

        // 경기 결과/스코어가 없을 경우만 예매 가능
        boolean notEnded = (result == null || result.isBlank()) && (score == null || score.isBlank());

        return withinPeriod && notEnded;
    }
}
