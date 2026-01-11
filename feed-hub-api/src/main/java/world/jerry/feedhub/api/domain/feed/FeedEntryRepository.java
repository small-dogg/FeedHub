package world.jerry.feedhub.api.domain.feed;

import java.util.Optional;

public interface FeedEntryRepository {

    FeedEntry save(FeedEntry feedEntry);

    Optional<FeedEntry> findById(Long id);

    boolean existsByRssInfoIdAndGuid(Long rssInfoId, String guid);
}
