package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.common.domain.BlogType;
import world.jerry.feedhub.common.domain.CrawlMode;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.kafka.BlogTypeDetector;
import world.jerry.feedhub.api.infrastructure.kafka.CrawlRequestProducer;
import world.jerry.feedhub.api.infrastructure.rss.FeedMetadata;
import world.jerry.feedhub.api.infrastructure.rss.RssParser;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RssInfoService {

    private final RssInfoRepository rssInfoRepository;
    private final CollectorCommandService collectorCommandService;
    private final CrawlRequestProducer crawlRequestProducer;
    private final BlogTypeDetector blogTypeDetector;
    private final RssParser rssParser;

    @Transactional
    public RssInfoDetail registerRssInfo(RegisterRssInfoCommand command) {
        if (rssInfoRepository.existsByRssUrl(command.rssUrl())) {
            throw new IllegalArgumentException("RSS source with URL '" + command.rssUrl() + "' already exists");
        }

        // RSS 피드에서 메타데이터 자동 수집
        FeedMetadata metadata = rssParser.fetchMetadata(command.rssUrl())
                .orElseThrow(() -> new IllegalArgumentException("RSS 피드를 읽을 수 없습니다: " + command.rssUrl()));

        // 수동 입력값 우선, 없으면 피드 메타데이터 사용
        String blogName = hasValue(command.blogName()) ? command.blogName() : metadata.title();
        String author = hasValue(command.author()) ? command.author() : metadata.author();
        String siteUrl = hasValue(command.siteUrl()) ? command.siteUrl() : metadata.siteUrl();
        String language = hasValue(command.language()) ? command.language() : metadata.language();

        // blogName이 여전히 없으면 RSS URL에서 추출
        if (!hasValue(blogName)) {
            blogName = extractBlogNameFromUrl(command.rssUrl());
        }

        // 수동 지정된 blogType이 없으면 자동 감지
        BlogType blogType = command.blogType() != null
                ? command.blogType()
                : blogTypeDetector.detect(siteUrl, command.rssUrl());

        RssInfo rssInfo = new RssInfo(
                blogName,
                author,
                command.rssUrl(),
                siteUrl,
                command.crawlUrl(),
                language,
                blogType
        );

        RssInfo saved = rssInfoRepository.save(rssInfo);

        // 최초 등록 시 RSS 동기화 Command 발행 (비동기)
        try {
            collectorCommandService.requestSync(saved.getId(), "api:register");
        } catch (Exception e) {
            log.warn("RSS 동기화 Command 발행 실패: id={}, blogName={}, error={}",
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

    /**
     * RSS 소스 정보 수정
     */
    @Transactional
    public RssInfoDetail updateRssInfo(Long id, String blogName, String author, String siteUrl, String crawlUrl, String language, BlogType blogType) {
        RssInfo rssInfo = rssInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RSS source not found with id: " + id));

        rssInfo.update(blogName, author, siteUrl, crawlUrl, language, blogType);
        return RssInfoDetail.from(rssInfo);
    }

    /**
     * 모든 RSS 소스의 blogType을 자동 감지로 갱신
     */
    @Transactional
    public List<RssInfoDetail> refreshAllBlogTypes() {
        List<RssInfo> allRssInfos = rssInfoRepository.findAll();

        for (RssInfo rssInfo : allRssInfos) {
            BlogType detectedType = blogTypeDetector.detect(rssInfo.getSiteUrl(), rssInfo.getRssUrl());
            rssInfo.updateBlogType(detectedType);
            log.info("Updated blogType for RSS source: id={}, blogName={}, blogType={}",
                    rssInfo.getId(), rssInfo.getBlogName(), detectedType);
        }

        return allRssInfos.stream()
                .map(RssInfoDetail::from)
                .toList();
    }

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }

    private String extractBlogNameFromUrl(String rssUrl) {
        try {
            String host = java.net.URI.create(rssUrl).getHost();
            if (host != null) {
                // www. 제거
                if (host.startsWith("www.")) {
                    host = host.substring(4);
                }
                return host;
            }
        } catch (Exception e) {
            log.warn("URL에서 블로그 이름 추출 실패: {}", rssUrl);
        }
        return "Unknown Blog";
    }
}
