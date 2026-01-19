package world.jerry.feedhub.crawler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.crawler.domain.FeedEntry;

/**
 * 피드 엔트리 레포지토리
 */
public interface FeedEntryRepository extends JpaRepository<FeedEntry, Long> {

    boolean existsByLink(String link);

    boolean existsByGuid(String guid);
}
