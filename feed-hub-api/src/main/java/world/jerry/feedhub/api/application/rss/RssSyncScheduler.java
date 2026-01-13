package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.api.application.rss.dto.SyncResult;

import java.util.List;

/**
 * RSS 동기화 스케줄러
 * 매일 정오(12:00)와 자정(00:00)에 전체 RSS 동기화를 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RssSyncScheduler {

    private final RssSyncService rssSyncService;

    /**
     * 매일 정오(12:00)에 전체 RSS 동기화 수행
     */
    @Scheduled(cron = "0 0 12 * * *")
    public void syncAtNoon() {
        log.info("정오 RSS 전체 동기화 시작");
        syncAll();
    }

    /**
     * 매일 자정(00:00)에 전체 RSS 동기화 수행
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void syncAtMidnight() {
        log.info("자정 RSS 전체 동기화 시작");
        syncAll();
    }

    private void syncAll() {
        try {
            List<SyncResult> results = rssSyncService.syncAllRssSources();
            int totalSynced = results.stream().mapToInt(SyncResult::syncedCount).sum();
            int totalSkipped = results.stream().mapToInt(SyncResult::skippedCount).sum();
            log.info("RSS 전체 동기화 완료: sources={}, synced={}, skipped={}",
                    results.size(), totalSynced, totalSkipped);
        } catch (Exception e) {
            log.error("RSS 전체 동기화 실패", e);
        }
    }
}
