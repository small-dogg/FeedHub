package world.jerry.feedhub.api.application.rss;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.common.command.Command;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Collector 서비스로 Command를 발행하는 서비스
 */
@Slf4j
@Service
public class CollectorCommandService {

    private static final String SYNC_RSS_TOPIC = "feedhub.commands.sync-rss";

    private final KafkaTemplate<String, Command> commandKafkaTemplate;
    private final RssInfoRepository rssInfoRepository;

    public CollectorCommandService(
            @Qualifier("commandKafkaTemplate") KafkaTemplate<String, Command> commandKafkaTemplate,
            RssInfoRepository rssInfoRepository) {
        this.commandKafkaTemplate = commandKafkaTemplate;
        this.rssInfoRepository = rssInfoRepository;
    }

    /**
     * RSS 동기화 Command 발행
     *
     * @param rssInfoId RSS 소스 ID
     * @param requestedBy 요청자 식별자 (예: "api:userId", "scheduler")
     * @return 발행된 Command ID
     */
    public String requestSync(Long rssInfoId, String requestedBy) {
        RssInfo rssInfo = rssInfoRepository.findById(rssInfoId)
                .orElseThrow(() -> new NoSuchElementException("RSS 소스를 찾을 수 없습니다: " + rssInfoId));

        String commandId = UUID.randomUUID().toString();

        SyncRssFeedCommand command = new SyncRssFeedCommand(
                commandId,
                rssInfo.getId(),
                rssInfo.getRssUrl(),
                rssInfo.getBlogName(),
                Instant.now(),
                requestedBy
        );

        CompletableFuture<SendResult<String, Command>> future =
                commandKafkaTemplate.send(SYNC_RSS_TOPIC, rssInfoId.toString(), command);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send SyncRssFeedCommand: commandId={}, rssInfoId={}, error={}",
                        commandId, rssInfoId, ex.getMessage());
            } else {
                log.info("SyncRssFeedCommand sent: commandId={}, rssInfoId={}, topic={}, partition={}, offset={}",
                        commandId,
                        rssInfoId,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return commandId;
    }

    /**
     * 전체 RSS 동기화 Command 발행
     *
     * @param requestedBy 요청자 식별자
     */
    public void requestSyncAll(String requestedBy) {
        rssInfoRepository.findAll().forEach(rssInfo -> {
            try {
                requestSync(rssInfo.getId(), requestedBy);
            } catch (Exception e) {
                log.error("Failed to send sync command for rssInfoId={}: {}",
                        rssInfo.getId(), e.getMessage());
            }
        });
    }
}
