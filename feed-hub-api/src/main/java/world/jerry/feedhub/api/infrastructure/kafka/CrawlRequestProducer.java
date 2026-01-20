package world.jerry.feedhub.api.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.api.domain.rss.BlogType;
import world.jerry.feedhub.api.domain.rss.CrawlMode;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.infrastructure.kafka.message.CrawlRequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 블로그 크롤링 요청을 Kafka로 발행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlRequestProducer {

    private final KafkaTemplate<String, CrawlRequestMessage> kafkaTemplate;
    private final BlogTypeDetector blogTypeDetector;

    /**
     * RSS 정보를 기반으로 크롤링 요청 발행
     * UNKNOWN 타입은 크롤링 대상이 아니므로 발행하지 않음
     */
    public void sendCrawlRequest(RssInfo rssInfo, CrawlMode crawlMode) {
        BlogType blogType;
        if (rssInfo.getBlogType().equals(BlogType.UNKNOWN)) {
            blogType = blogTypeDetector.detect(rssInfo.getSiteUrl(), rssInfo.getRssUrl());
        } else {
            blogType = rssInfo.getBlogType();
        }

        if (blogType == BlogType.UNKNOWN) {
            log.debug("Skipping crawl request for UNKNOWN blog type: rssInfoId={}", rssInfo.getId());
            return;
        }

        String topicName = blogTypeDetector.getTopicName(blogType);
        if (topicName == null) {
            log.warn("No topic found for blog type: {}", blogType);
            return;
        }

        CrawlRequestMessage message = CrawlRequestMessage.of(
                rssInfo.getId(),
                rssInfo.getBlogName(),
                blogType,
                rssInfo.getSiteUrl(),
                rssInfo.getCrawlUrl(),
                rssInfo.getRssUrl(),
                crawlMode
        );

        // 파티션 키로 rssInfoId 사용하여 동일 RSS의 메시지가 같은 파티션으로 전송되도록 함
        String partitionKey = String.valueOf(rssInfo.getId());

        CompletableFuture<SendResult<String, CrawlRequestMessage>> future =
                kafkaTemplate.send(topicName, partitionKey, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send crawl request: rssInfoId={}, blogType={}, error={}",
                        rssInfo.getId(), blogType, ex.getMessage());
            } else {
                log.info("Crawl request sent: rssInfoId={}, blogType={}, topic={}, partition={}, offset={}",
                        rssInfo.getId(),
                        blogType,
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
