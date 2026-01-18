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
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.QFeedEntry;
import world.jerry.feedhub.api.domain.rss.QRssInfo;
import world.jerry.feedhub.api.domain.tag.QTag;
import world.jerry.feedhub.api.domain.tag.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Filter by tags (OR logic) - feeds that have ANY of the specified tags
        if (criteria.tagIds() != null && !criteria.tagIds().isEmpty()) {
            predicate.and(feed.id.in(
                    JPAExpressions
                            .select(feed.id)
                            .from(feed)
                            .join(feed.tags, t)
                            .where(t.id.in(criteria.tagIds()))
            ));
        }

        // Text search on title and description (uses pg_trgm GIN index)
        if (criteria.query() != null && !criteria.query().isBlank()) {
            String searchPattern = "%" + criteria.query().trim() + "%";
            predicate.and(
                    feed.title.likeIgnoreCase(searchPattern)
                            .or(feed.description.likeIgnoreCase(searchPattern))
            );
        }

        // Cursor-based pagination for publishedAt DESC, id DESC ordering
        // WHERE publishedAt < lastPublishedAt OR (publishedAt = lastPublishedAt AND id < lastId)
        if (criteria.lastPublishedAt() != null && criteria.lastId() != null) {
            predicate.and(
                    feed.publishedAt.lt(criteria.lastPublishedAt())
                            .or(feed.publishedAt.eq(criteria.lastPublishedAt())
                                    .and(feed.id.lt(criteria.lastId())))
            );
        } else if (criteria.lastId() != null) {
            // Fallback for feeds without publishedAt cursor
            predicate.and(feed.id.lt(criteria.lastId()));
        }

        // Fetch one more to check if there are more results
        int fetchSize = criteria.size() + 1;

        List<Tuple> results = queryFactory
                .select(feed, rss.blogName, rss.siteUrl)
                .from(feed)
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .where(predicate)
                .orderBy(feed.publishedAt.desc().nullsLast(), feed.id.desc())
                .limit(fetchSize)
                .fetch();

        boolean hasMore = results.size() > criteria.size();
        if (hasMore) {
            results = results.subList(0, criteria.size());
        }

        // Collect unique feed entry IDs for batch tag loading
        Set<Long> feedEntryIds = results.stream()
                .map(tuple -> tuple.get(feed))
                .filter(f -> f != null)
                .map(FeedEntry::getId)
                .collect(Collectors.toSet());

        // Fetch tags for all feed entries in one query
        Map<Long, Set<Tag>> tagsByFeedEntryId = fetchTagsByFeedEntryIds(feedEntryIds);

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> {
                    FeedEntry entry = tuple.get(feed);
                    String blogName = tuple.get(rss.blogName);
                    String siteUrl = tuple.get(rss.siteUrl);
                    Set<Tag> tags = tagsByFeedEntryId.getOrDefault(entry.getId(), Set.of());
                    return FeedEntryInfo.from(entry, blogName, siteUrl, tags);
                })
                .toList();

        return FeedEntryPage.of(content, hasMore);
    }

    private Map<Long, Set<Tag>> fetchTagsByFeedEntryIds(Set<Long> feedEntryIds) {
        if (feedEntryIds.isEmpty()) {
            return Map.of();
        }

        List<FeedEntry> feedEntries = queryFactory
                .selectFrom(feedEntry)
                .leftJoin(feedEntry.tags, tag).fetchJoin()
                .where(feedEntry.id.in(feedEntryIds))
                .fetch();

        return feedEntries.stream()
                .collect(Collectors.toMap(
                        FeedEntry::getId,
                        f -> new HashSet<>(f.getTags())
                ));
    }
}
