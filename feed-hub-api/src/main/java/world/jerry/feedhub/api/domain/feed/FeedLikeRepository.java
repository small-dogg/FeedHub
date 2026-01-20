package world.jerry.feedhub.api.domain.feed;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface FeedLikeRepository extends JpaRepository<FeedLike, Long> {

    Optional<FeedLike> findByMemberIdAndFeedEntryId(Long memberId, Long feedEntryId);

    boolean existsByMemberIdAndFeedEntryId(Long memberId, Long feedEntryId);

    void deleteByMemberIdAndFeedEntryId(Long memberId, Long feedEntryId);

    /**
     * 특정 피드의 좋아요 수 조회
     */
    long countByFeedEntryId(Long feedEntryId);

    /**
     * 여러 피드의 좋아요 수를 한 번에 조회
     */
    @Query("SELECT f.feedEntryId, COUNT(f) FROM FeedLike f WHERE f.feedEntryId IN :feedEntryIds GROUP BY f.feedEntryId")
    Set<Object[]> countByFeedEntryIdIn(@Param("feedEntryIds") Set<Long> feedEntryIds);

    /**
     * 특정 회원이 좋아요한 피드 ID 목록 조회
     */
    @Query("SELECT f.feedEntryId FROM FeedLike f WHERE f.memberId = :memberId AND f.feedEntryId IN :feedEntryIds")
    Set<Long> findLikedFeedEntryIdsByMemberIdAndFeedEntryIdIn(
            @Param("memberId") Long memberId,
            @Param("feedEntryIds") Set<Long> feedEntryIds);
}
