package world.jerry.feedhub.api.application.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;
import world.jerry.feedhub.api.application.feed.dto.FeedSearchCriteria;
import world.jerry.feedhub.api.infrastructure.persistence.feed.FeedEntryQueryRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeedQueryService {

    private final FeedEntryQueryRepository feedEntryQueryRepository;

    public FeedEntryPage searchFeeds(FeedSearchCriteria criteria) {
        return feedEntryQueryRepository.searchFeeds(criteria);
    }
}
