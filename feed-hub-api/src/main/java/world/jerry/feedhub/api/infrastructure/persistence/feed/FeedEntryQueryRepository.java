package world.jerry.feedhub.api.infrastructure.persistence.feed;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryInfo;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;
import world.jerry.feedhub.api.application.feed.dto.FeedSearchCriteria;
import world.jerry.feedhub.api.domain.feed.QFeedEntry;
import world.jerry.feedhub.api.domain.rss.QRssInfo;
import world.jerry.feedhub.api.domain.tag.QTag;

import java.util.List;

import static world.jerry.feedhub.api.domain.feed.QFeedEntry.feedEntry;
import static world.jerry.feedhub.api.domain.rss.QRssInfo.rssInfo;
import static world.jerry.feedhub.api.domain.tag.QTag.tag;

@Repository
@RequiredArgsConstructor
public class FeedEntryQueryRepository {

    private final JPAQueryFactory queryFactory;

    public FeedEntryPage searchFeeds(FeedSearchCriteria criteria) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;
        QTag t = tag;

        BooleanBuilder predicate = new BooleanBuilder();

        // Filter by RSS source IDs
        if (criteria.rssInfoIds() != null && !criteria.rssInfoIds().isEmpty()) {
            predicate.and(feed.rssInfoId.in(criteria.rssInfoIds()));
        }

        // Filter by tags (OR logic) - feeds from RSS sources that have ANY of the specified tags
        if (criteria.tagIds() != null && !criteria.tagIds().isEmpty()) {
            predicate.and(feed.rssInfoId.in(
                    JPAExpressions
                            .select(rss.id)
                            .from(rss)
                            .join(rss.tags, t)
                            .where(t.id.in(criteria.tagIds()))
            ));
        }

        // Cursor-based pagination: fetch items with id < lastId
        if (criteria.lastId() != null) {
            predicate.and(feed.id.lt(criteria.lastId()));
        }

        // Fetch one more to check if there are more results
        int fetchSize = criteria.size() + 1;

        List<Tuple> results = queryFactory
                .select(feed, rss.blogName)
                .from(feed)
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .where(predicate)
                .orderBy(feed.id.desc())
                .limit(fetchSize)
                .fetch();

        boolean hasMore = results.size() > criteria.size();
        if (hasMore) {
            results = results.subList(0, criteria.size());
        }

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> FeedEntryInfo.from(
                        tuple.get(feed),
                        tuple.get(rss.blogName)
                ))
                .toList();

        return FeedEntryPage.of(content, hasMore);
    }
}
