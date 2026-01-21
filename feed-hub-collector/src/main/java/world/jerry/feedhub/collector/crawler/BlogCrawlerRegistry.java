package world.jerry.feedhub.collector.crawler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import world.jerry.feedhub.common.domain.BlogType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 블로그 타입에 따른 크롤러 선택 (Strategy Pattern)
 */
@Slf4j
@Component
public class BlogCrawlerRegistry {

    private final Map<BlogType, BlogCrawler> crawlerMap;

    public BlogCrawlerRegistry(List<BlogCrawler> crawlers) {
        this.crawlerMap = crawlers.stream()
                .collect(Collectors.toMap(
                        BlogCrawler::getSupportedType,
                        Function.identity()
                ));
        log.info("Registered {} blog crawlers: {}",
                crawlerMap.size(),
                crawlerMap.keySet());
    }

    /**
     * 블로그 타입에 해당하는 크롤러 반환
     */
    public Optional<BlogCrawler> getCrawler(BlogType blogType) {
        return Optional.ofNullable(crawlerMap.get(blogType));
    }

    /**
     * 지원하는 모든 블로그 타입 반환
     */
    public List<BlogType> getSupportedTypes() {
        return List.copyOf(crawlerMap.keySet());
    }
}
