package world.jerry.feedhub.api.infrastructure.persistence.rss;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;

public interface JpaRssInfoRepository extends JpaRepository<RssInfo, Long>, RssInfoRepository {

    @Override
    boolean existsByRssUrl(String rssUrl);
}
