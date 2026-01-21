package world.jerry.feedhub.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.collector.domain.RssInfo;

/**
 * RSS source repository (read-only operations in Collector)
 */
public interface RssInfoRepository extends JpaRepository<RssInfo, Long> {
}
