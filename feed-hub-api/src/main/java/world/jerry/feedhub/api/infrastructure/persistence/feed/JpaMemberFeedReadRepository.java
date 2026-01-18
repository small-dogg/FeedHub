package world.jerry.feedhub.api.infrastructure.persistence.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import world.jerry.feedhub.api.domain.feed.MemberFeedRead;
import world.jerry.feedhub.api.domain.feed.MemberFeedReadRepository;

import java.util.Set;

public interface JpaMemberFeedReadRepository extends JpaRepository<MemberFeedRead, Long>, MemberFeedReadRepository {

    @Override
    boolean existsByMemberIdAndFeedEntryId(Long memberId, Long feedEntryId);

    @Override
    @Query("SELECT mfr.feedEntryId FROM MemberFeedRead mfr WHERE mfr.memberId = :memberId AND mfr.feedEntryId IN :feedEntryIds")
    Set<Long> findReadFeedEntryIdsByMemberIdAndFeedEntryIdIn(@Param("memberId") Long memberId, @Param("feedEntryIds") Set<Long> feedEntryIds);
}
