package world.jerry.feedhub.collector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.collector.domain.SyncHistory;

/**
 * 동기화 이력 Repository
 */
public interface SyncHistoryRepository extends JpaRepository<SyncHistory, Long> {
}
