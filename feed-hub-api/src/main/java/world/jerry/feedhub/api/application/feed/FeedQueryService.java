package world.jerry.feedhub.api.application.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;
import world.jerry.feedhub.api.application.feed.dto.FeedSearchCriteria;
import world.jerry.feedhub.api.domain.subscription.Subscription;
import world.jerry.feedhub.api.domain.subscription.SubscriptionRepository;
import world.jerry.feedhub.api.infrastructure.persistence.feed.FeedEntryQueryRepository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedQueryService {

    private final FeedEntryQueryRepository feedEntryQueryRepository;
    private final SubscriptionRepository subscriptionRepository;

    public FeedEntryPage searchFeeds(FeedSearchCriteria criteria) {
        if (Boolean.TRUE.equals(criteria.followedOnly()) && criteria.memberId() != null) {
            List<Subscription> subscriptions = subscriptionRepository.findAllByMemberId(criteria.memberId());
            if (subscriptions.isEmpty()) {
                return FeedEntryPage.of(Collections.emptyList(), false);
            }

            List<Long> subscribedRssIds = subscriptions.stream()
                    .map(sub -> sub.getRssInfo().getId())
                    .collect(Collectors.toList());

            // 만약 검색 조건에 이미 RSS ID가 지정되어 있다면 교집합(내 구독 중 선택한 RSS)
            List<Long> requestRssIds = criteria.rssInfoIds();
            if (requestRssIds != null && !requestRssIds.isEmpty()) {
                subscribedRssIds = subscribedRssIds.stream()
                        .filter(id -> requestRssIds.contains(id))
                        .collect(Collectors.toList());

                if (subscribedRssIds.isEmpty()) {
                    return FeedEntryPage.of(Collections.emptyList(), false);
                }
            }

            // Criteria 재정의 (RSS ID 목록 교체)
            criteria = new FeedSearchCriteria(
                    criteria.memberId(),
                    subscribedRssIds,
                    criteria.tagIds(),
                    criteria.query(),
                    criteria.lastId(),
                    criteria.lastPublishedAt(),
                    criteria.lastLikeCount(),
                    criteria.lastLikedAt(),
                    criteria.sortType(),
                    criteria.likedOnly(),
                    criteria.followedOnly(),
                    criteria.size());
        }

        return feedEntryQueryRepository.searchFeeds(criteria);
    }
}
