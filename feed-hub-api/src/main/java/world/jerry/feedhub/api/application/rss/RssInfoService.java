package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.domain.rss.CrawlMode;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.kafka.CrawlRequestProducer;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RssInfoService {

    private final RssInfoRepository rssInfoRepository;
    private final RssSyncService rssSyncService;
    private final CrawlRequestProducer crawlRequestProducer;

    @Transactional
    public RssInfoDetail registerRssInfo(RegisterRssInfoCommand command) {
        if (rssInfoRepository.existsByRssUrl(command.rssUrl())) {
            throw new IllegalArgumentException("RSS source with URL '" + command.rssUrl() + "' already exists");
        }

        RssInfo rssInfo = new RssInfo(
                command.blogName(),
                command.author(),
                command.rssUrl(),
                command.siteUrl(),
                command.language()
        );

        RssInfo saved = rssInfoRepository.save(rssInfo);

        // 최초 등록 시 RSS 동기화 수행
        try {
            rssSyncService.syncRssSource(saved.getId());
        } catch (Exception e) {
            log.warn("RSS 최초 동기화 실패: id={}, blogName={}, error={}",
                    saved.getId(), saved.getBlogName(), e.getMessage());
        }

        // 블로그 크롤링 요청 발행 (비동기)
        try {
            crawlRequestProducer.sendCrawlRequest(saved, CrawlMode.FULL);
        } catch (Exception e) {
            log.warn("크롤링 요청 발행 실패: id={}, blogName={}, error={}",
                    saved.getId(), saved.getBlogName(), e.getMessage());
        }

        return RssInfoDetail.from(saved);
    }

    public RssInfoDetail getRssInfo(Long id) {
        RssInfo rssInfo = rssInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RSS source not found with id: " + id));
        return RssInfoDetail.from(rssInfo);
    }

    public List<RssInfoDetail> getAllRssInfos() {
        return rssInfoRepository.findAll().stream()
                .map(RssInfoDetail::from)
                .toList();
    }

    @Transactional
    public void unregisterRssInfo(Long id) {
        if (rssInfoRepository.findById(id).isEmpty()) {
            throw new NoSuchElementException("RSS source not found with id: " + id);
        }
        rssInfoRepository.deleteById(id);
    }
}
