package world.jerry.feedhub.collector.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.collector.crawler.BlogCrawler;
import world.jerry.feedhub.collector.crawler.BlogCrawlerRegistry;
import world.jerry.feedhub.collector.domain.CrawlResult;
import world.jerry.feedhub.collector.domain.SyncHistory;
import world.jerry.feedhub.collector.message.CrawlRequestMessage;
import world.jerry.feedhub.collector.repository.SyncHistoryRepository;
import world.jerry.feedhub.collector.service.FeedEntrySaveService;

import java.util.Optional;
import java.util.UUID;

/**
 * Kafka 크롤링 요청 메시지 Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlRequestConsumer {

    private final BlogCrawlerRegistry crawlerRegistry;
    private final FeedEntrySaveService feedEntrySaveService;
    private final SyncHistoryRepository syncHistoryRepository;

    @Transactional
    @KafkaListener(topics = "tistory-crawl", groupId = "feed-hub-collector-group")
    public void consumeTistory(ConsumerRecord<String, CrawlRequestMessage> record, Acknowledgment ack) {
        processMessage(record, ack);
    }

    @Transactional
    @KafkaListener(topics = "medium-crawl", groupId = "feed-hub-collector-group")
    public void consumeMedium(ConsumerRecord<String, CrawlRequestMessage> record, Acknowledgment ack) {
        processMessage(record, ack);
    }

    @Transactional
    @KafkaListener(topics = "velog-crawl", groupId = "feed-hub-collector-group")
    public void consumeVelog(ConsumerRecord<String, CrawlRequestMessage> record, Acknowledgment ack) {
        processMessage(record, ack);
    }

    @Transactional
    @KafkaListener(topics = "github-blog-crawl", groupId = "feed-hub-collector-group")
    public void consumeGithubBlog(ConsumerRecord<String, CrawlRequestMessage> record, Acknowledgment ack) {
        processMessage(record, ack);
    }

    private void processMessage(ConsumerRecord<String, CrawlRequestMessage> record, Acknowledgment ack) {
        CrawlRequestMessage message = record.value();
        String commandId = UUID.randomUUID().toString();

        log.info("Received crawl request: rssInfoId={}, blogType={}, topic={}",
                message.rssInfoId(), message.blogType(), record.topic());

        // 크롤링 이력 시작
        SyncHistory history = SyncHistory.started(
                commandId,
                message.rssInfoId(),
                "CRAWL",
                "api:crawl"
        );
        syncHistoryRepository.save(history);

        try {
            Optional<BlogCrawler> crawlerOpt = crawlerRegistry.getCrawler(message.blogType());

            if (crawlerOpt.isEmpty()) {
                log.warn("No crawler found for blog type: {}", message.blogType());
                history.fail("No crawler found for blog type: " + message.blogType());
                syncHistoryRepository.save(history);
                ack.acknowledge();
                return;
            }

            BlogCrawler crawler = crawlerOpt.get();
            CrawlResult result = crawler.crawl(message);

            if (result.success()) {
                int savedCount = feedEntrySaveService.saveAll(result.articles(), message.rssInfoId());

                // 크롤링 이력 완료
                history.complete(savedCount, result.getArticleCount() - savedCount);
                syncHistoryRepository.save(history);

                log.info("Crawl completed: rssInfoId={}, crawled={}, saved={}",
                        message.rssInfoId(), result.getArticleCount(), savedCount);
            } else {
                // 크롤링 이력 실패
                history.fail(result.errorMessage());
                syncHistoryRepository.save(history);

                log.error("Crawl failed: rssInfoId={}, error={}",
                        message.rssInfoId(), result.errorMessage());
            }

            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process crawl request: rssInfoId={}, error={}",
                    message.rssInfoId(), e.getMessage(), e);

            // 크롤링 이력 실패
            history.fail(e.getMessage());
            syncHistoryRepository.save(history);

            ack.acknowledge();
        }
    }
}
