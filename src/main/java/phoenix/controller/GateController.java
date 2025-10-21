package phoenix.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import phoenix.model.dto.GateDto;
import phoenix.service.GateService;

import java.util.Map;

// 대기열 및 입장 제어를 담당하는 컨트롤러 클래승
@RestController
@RequestMapping("/gate")
@RequiredArgsConstructor
public class GateController {   // class start

    // GateService 의존성 주입
    private final GateService gateService;

    // 대기열 등록 메소드
    // 사용자가 예매 페이지 입장 를 했을 때 호출되는 로직
    // 요청: EnqueueRequest(mno, showId)
    // 응답: EnqueueResponse(queued, waiting)
    @PostMapping("/enqueue")
    public ResponseEntity<GateDto.EnqueueResponse> enqueue (@RequestBody GateDto.EnqueueRequest req) {
        // 서비스 호출 (실제 대기열 등록 로직 실행)
        GateService.EnqueueResult res = gateService.enqueue(req.getMno(), req.getGno());

        // 응답 DTO로 변환하여 반환 (queued = 등록여부, waiting = 대기열 길이)
        return ResponseEntity.ok(new GateDto.EnqueueResponse(res.queued(), res.waiting()));
    }   // func end


    // 퇴장 처리 메소드
    // 사용자가 예매를 마쳤거나 브라우저를 닫을 때 호출된다
    // 요청: LeaveRequest(mno)
    // 응답: LeaveResponse(success)
    // consumes
    @PostMapping(value = "/leave", consumes = MediaType.ALL_VALUE)
    public ResponseEntity<GateDto.LeaveResponse> leave(@RequestBody GateDto.LeaveRequest req) {
        boolean ok = gateService.leave(req.getMno());
        return ResponseEntity.ok(new GateDto.LeaveResponse(ok));
    }   // func end


    // 상태 조회 메소드
    // 현재 게이트(대기열)의 상태를 조회하는 엔드포인트
    // 대기열에 몇 명이 남아 있는지 (queue.size)
    // 현재 세마포어에 남은 입장 가능 슬롯 수 (availablePermits)
    // 응답: StatusResponse(waitingCount, availablePermits)
    @GetMapping("/status")
    public ResponseEntity<GateDto.StatusResponse> status() {
        return ResponseEntity.ok(new GateDto.StatusResponse(
                gateService.waitingCount(),      // 대기열 인원 수
                gateService.availablePermits()   // 현재 남은 입장 가능 수
        ));
    }   // func end


    // 세션 연장 메소드
    // 사용자가 예매 도중 "연장하기" 버튼을 눌렀을 때 호출
    // 세션 TTL(유효시간)을 +1분 연장
    // 단, 최대 2회까지만 가능 (3번째 시도는 false 반환)
    // TTL이 만료된 세션은 연장 불가
    // 응답: 연장 결과 코드 (1=1회차 성공, 2=2회차 성공, -1=실패)
    @PostMapping("/extend/{mno}")
    public ResponseEntity<Integer> extend(@PathVariable int mno) {
        int result = gateService.extendSession(mno);
        return ResponseEntity.ok(result);
    }   // func end


    // 프론트에서 확인할 것 세션이 있는지! 주기적 확인
    @GetMapping("/check/{mno}")
    public ResponseEntity<Map<String, Boolean>> check(@PathVariable int mno) {
        boolean ready = gateService.isEntered(mno);
        return ResponseEntity.ok(Map.of("ready", ready));
    }   // func end

    // 내가 대기열 몇 번째인지 알려주는 메소드
    @GetMapping("/position/{mno}")
    public ResponseEntity<Map<String, Integer>> position(@PathVariable int mno) {
        Integer pos = gateService.positionOf(mno);
        // 정책: null → -1 로 내려 “대기열에 없음” 표현
        return ResponseEntity.ok(Map.of("position", pos == null ? -1 : pos));
    }   // func end

}   // class end
