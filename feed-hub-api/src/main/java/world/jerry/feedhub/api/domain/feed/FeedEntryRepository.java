package world.jerry.feedhub.api.domain.feed;

import java.util.Optional;

public interface FeedEntryRepository {

    FeedEntry save(FeedEntry feedEntry);

    Optional<FeedEntry> findById(Long id);

    boolean existsByRssInfoIdAndGuid(Long rssInfoId, String guid);

    /**
     * 조회수를 원자적으로 1 증가시킴 (동시성 안전)
     * @return 업데이트된 행 수
     */
    int incrementViewCount(Long id);
}
