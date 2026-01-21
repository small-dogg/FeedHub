package world.jerry.feedhub.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.collector.domain.FeedEntry;

/**
 * 피드 엔트리 레포지토리
 */
public interface FeedEntryRepository extends JpaRepository<FeedEntry, Long> {

    boolean existsByLink(String link);

    boolean existsByGuid(String guid);
}
