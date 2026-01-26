package world.jerry.feedhub.api.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberIdAndRssInfoId(Long memberId, Long rssInfoId);

    Optional<Subscription> findByMemberIdAndRssInfoId(Long memberId, Long rssInfoId);

    List<Subscription> findAllByMemberId(Long memberId);
}
