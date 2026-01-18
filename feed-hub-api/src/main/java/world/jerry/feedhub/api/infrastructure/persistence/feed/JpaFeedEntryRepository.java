package world.jerry.feedhub.api.infrastructure.persistence.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.FeedEntryRepository;

public interface JpaFeedEntryRepository extends JpaRepository<FeedEntry, Long>, FeedEntryRepository {

    @Override
    boolean existsByRssInfoIdAndGuid(Long rssInfoId, String guid);

    @Override
    @Modifying
    @Query("UPDATE FeedEntry f SET f.viewCount = f.viewCount + 1 WHERE f.id = :id")
    int incrementViewCount(@Param("id") Long id);
}
