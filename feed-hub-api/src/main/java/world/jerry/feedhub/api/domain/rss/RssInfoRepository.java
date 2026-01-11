package world.jerry.feedhub.api.domain.rss;

import java.util.List;
import java.util.Optional;

public interface RssInfoRepository {

    RssInfo save(RssInfo rssInfo);

    Optional<RssInfo> findById(Long id);

    List<RssInfo> findAll();

    void deleteById(Long id);

    boolean existsByRssUrl(String rssUrl);
}
