package world.jerry.feedhub.api.infrastructure.persistence.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.FeedEntryRepository;

public interface JpaFeedEntryRepository extends JpaRepository<FeedEntry, Long>, FeedEntryRepository {

    @Override
    boolean existsByRssInfoIdAndGuid(Long rssInfoId, String guid);
}
