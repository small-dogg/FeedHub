package world.jerry.feedhub.api.domain.feed;

import java.util.Set;

/**
 * 회원 피드 읽음 기록 저장소
 */
public interface MemberFeedReadRepository {

    MemberFeedRead save(MemberFeedRead memberFeedRead);

    boolean existsByMemberIdAndFeedEntryId(Long memberId, Long feedEntryId);

    Set<Long> findReadFeedEntryIdsByMemberIdAndFeedEntryIdIn(Long memberId, Set<Long> feedEntryIds);
}
