package phoenix.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    // 예매 가능 여부 계산
    public boolean isReservable() {
        LocalDateTime gameDateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        // 경기 시작 7일 전부터 예매 가능, 경기 시작 전까지만 가능
        boolean withinPeriod = now.isAfter(gameDateTime.minusDays(7)) && now.isBefore(gameDateTime);
        boolean notEnded = (result == null || result.isBlank()) && (score == null || score.isBlank());

        return withinPeriod && notEnded;
    }
}