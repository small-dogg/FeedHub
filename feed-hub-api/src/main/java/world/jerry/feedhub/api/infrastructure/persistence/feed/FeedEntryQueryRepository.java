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
            // Use subquery: select rss_info.id from rss_info join rss_info.tags where tag.id in (...)
            predicate.and(feed.rssInfoId.in(
                    JPAExpressions
                            .select(rss.id)
                            .from(rss)
                            .join(rss.tags, t)
                            .where(t.id.in(criteria.tagIds()))
            ));
        }

        // Get total count
        Long total = queryFactory
                .select(feed.count())
                .from(feed)
                .where(predicate)
                .fetchOne();

        if (total == null) {
            total = 0L;
        }

        // Get paginated results with blog name
        List<Tuple> results = queryFactory
                .select(feed, rss.blogName)
                .from(feed)
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .where(predicate)
                .orderBy(feed.publishedAt.desc().nullsLast())
                .offset(criteria.offset())
                .limit(criteria.size())
                .fetch();

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> FeedEntryInfo.from(
                        tuple.get(feed),
                        tuple.get(rss.blogName)
                ))
                .toList();

        return FeedEntryPage.of(content, criteria.page(), criteria.size(), total);
    }
}
