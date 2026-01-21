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
import world.jerry.feedhub.api.application.feed.dto.FeedSortType;
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.FeedLikeRepository;
import world.jerry.feedhub.api.domain.feed.MemberFeedReadRepository;
import world.jerry.feedhub.api.domain.feed.QFeedEntry;
import world.jerry.feedhub.api.domain.feed.QFeedLike;
import world.jerry.feedhub.api.domain.rss.QRssInfo;
import world.jerry.feedhub.api.domain.tag.QTag;
import world.jerry.feedhub.api.domain.tag.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static world.jerry.feedhub.api.domain.feed.QFeedEntry.feedEntry;
import static world.jerry.feedhub.api.domain.feed.QFeedLike.feedLike;
import static world.jerry.feedhub.api.domain.rss.QRssInfo.rssInfo;
import static world.jerry.feedhub.api.domain.tag.QTag.tag;

@Repository
@RequiredArgsConstructor
public class FeedEntryQueryRepository {

    private final JPAQueryFactory queryFactory;
    private final MemberFeedReadRepository memberFeedReadRepository;
    private final FeedLikeRepository feedLikeRepository;

    /**
     * 피드 검색 (sortType에 따라 다른 쿼리 실행)
     */
    public FeedEntryPage searchFeeds(FeedSearchCriteria criteria) {
        return switch (criteria.sortType()) {
            case PUBLISHED_AT -> searchFeedsByPublishedAt(criteria);
            case LIKE_COUNT -> searchFeedsByLikeCount(criteria);
            case LIKED_AT -> searchFeedsByLikedAt(criteria);
        };
    }

    /**
     * 게시날짜 순 정렬 (기존 방식)
     */
    private FeedEntryPage searchFeedsByPublishedAt(FeedSearchCriteria criteria) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;

        BooleanBuilder predicate = buildBasePredicate(criteria, feed);

        // likedOnly 필터
        if (Boolean.TRUE.equals(criteria.likedOnly()) && criteria.memberId() != null) {
            predicate.and(feed.id.in(
                    JPAExpressions
                            .select(feedLike.feedEntryId)
                            .from(feedLike)
                            .where(feedLike.memberId.eq(criteria.memberId()))
            ));
        }

        // Cursor-based pagination for publishedAt DESC, id DESC ordering
        if (criteria.lastPublishedAt() != null && criteria.lastId() != null) {
            predicate.and(
                    feed.publishedAt.lt(criteria.lastPublishedAt())
                            .or(feed.publishedAt.eq(criteria.lastPublishedAt())
                                    .and(feed.id.lt(criteria.lastId())))
            );
        } else if (criteria.lastId() != null) {
            predicate.and(feed.id.lt(criteria.lastId()));
        }

        int fetchSize = criteria.size() + 1;

        List<Tuple> results = queryFactory
                .select(feed, rss.blogName, rss.siteUrl)
                .from(feed)
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .where(predicate)
                .orderBy(feed.publishedAt.desc().nullsLast(), feed.id.desc())
                .limit(fetchSize)
                .fetch();

        return buildFeedEntryPage(results, criteria, FeedSortType.PUBLISHED_AT);
    }

    /**
     * 좋아요 수 순 정렬 (LEFT JOIN + GROUP BY 방식)
     */
    private FeedEntryPage searchFeedsByLikeCount(FeedSearchCriteria criteria) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;
        QFeedLike like = new QFeedLike("likeForCount");

        BooleanBuilder predicate = buildBasePredicate(criteria, feed);

        // likedOnly 필터
        if (Boolean.TRUE.equals(criteria.likedOnly()) && criteria.memberId() != null) {
            predicate.and(feed.id.in(
                    JPAExpressions
                            .select(feedLike.feedEntryId)
                            .from(feedLike)
                            .where(feedLike.memberId.eq(criteria.memberId()))
            ));
        }

        int fetchSize = criteria.size() + 1;

        // LEFT JOIN으로 좋아요 수를 직접 계산
        List<Tuple> results = queryFactory
                .select(feed, rss.blogName, rss.siteUrl, like.count())
                .from(feed)
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .leftJoin(like).on(like.feedEntryId.eq(feed.id))
                .where(predicate)
                .groupBy(feed.id, rss.blogName, rss.siteUrl)
                .orderBy(like.count().desc(), feed.publishedAt.desc().nullsLast(), feed.id.desc())
                .limit(fetchSize)
                .fetch();

        return buildFeedEntryPageWithLikeCount(results, criteria);
    }

    /**
     * 좋아요한 시간 순 정렬 (likedOnly=true 필수)
     */
    private FeedEntryPage searchFeedsByLikedAt(FeedSearchCriteria criteria) {
        // LIKED_AT 정렬은 로그인 사용자의 좋아요 목록에서만 유효
        if (criteria.memberId() == null) {
            return FeedEntryPage.of(List.of(), false, FeedSortType.LIKED_AT);
        }

        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;
        QFeedLike like = feedLike;

        BooleanBuilder predicate = buildBasePredicate(criteria, feed);

        // Cursor-based pagination for likedAt DESC, id DESC
        if (criteria.lastLikedAt() != null && criteria.lastId() != null) {
            predicate.and(
                    like.createdAt.lt(criteria.lastLikedAt())
                            .or(like.createdAt.eq(criteria.lastLikedAt())
                                    .and(feed.id.lt(criteria.lastId())))
            );
        }

        int fetchSize = criteria.size() + 1;

        // INNER JOIN으로 해당 회원이 좋아요한 피드만 조회
        List<Tuple> results = queryFactory
                .select(feed, rss.blogName, rss.siteUrl, like.createdAt)
                .from(feed)
                .innerJoin(like).on(like.feedEntryId.eq(feed.id).and(like.memberId.eq(criteria.memberId())))
                .leftJoin(rss).on(feed.rssInfoId.eq(rss.id))
                .where(predicate)
                .orderBy(like.createdAt.desc(), feed.id.desc())
                .limit(fetchSize)
                .fetch();

        return buildFeedEntryPageWithLikedAt(results, criteria);
    }

    /**
     * 공통 필터 조건 생성
     */
    private BooleanBuilder buildBasePredicate(FeedSearchCriteria criteria, QFeedEntry feed) {
        BooleanBuilder predicate = new BooleanBuilder();

        // Filter by RSS source IDs
        if (criteria.rssInfoIds() != null && !criteria.rssInfoIds().isEmpty()) {
            predicate.and(feed.rssInfoId.in(criteria.rssInfoIds()));
        }

        // Filter by tags (OR logic)
        if (criteria.tagIds() != null && !criteria.tagIds().isEmpty()) {
            predicate.and(feed.id.in(
                    JPAExpressions
                            .select(feed.id)
                            .from(feed)
                            .join(feed.tags, tag)
                            .where(tag.id.in(criteria.tagIds()))
            ));
        }

        // Text search
        if (criteria.query() != null && !criteria.query().isBlank()) {
            String searchPattern = "%" + criteria.query().trim() + "%";
            predicate.and(
                    feed.title.likeIgnoreCase(searchPattern)
                            .or(feed.description.likeIgnoreCase(searchPattern))
            );
        }

        return predicate;
    }

    /**
     * FeedEntryPage 생성 (PUBLISHED_AT, LIKE_COUNT 정렬용)
     */
    private FeedEntryPage buildFeedEntryPage(List<Tuple> results, FeedSearchCriteria criteria, FeedSortType sortType) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;

        boolean hasMore = results.size() > criteria.size();
        if (hasMore) {
            results = results.subList(0, criteria.size());
        }

        Set<Long> feedEntryIds = results.stream()
                .map(tuple -> tuple.get(feed))
                .filter(f -> f != null)
                .map(FeedEntry::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Tag>> tagsByFeedEntryId = fetchTagsByFeedEntryIds(feedEntryIds, criteria.memberId());
        Set<Long> readFeedEntryIds = fetchReadFeedEntryIds(feedEntryIds, criteria.memberId());
        Map<Long, Long> likeCounts = fetchLikeCounts(feedEntryIds);
        Set<Long> likedFeedEntryIds = fetchLikedFeedEntryIds(feedEntryIds, criteria.memberId());

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> {
                    FeedEntry entry = tuple.get(feed);
                    String blogName = tuple.get(rss.blogName);
                    String siteUrl = tuple.get(rss.siteUrl);
                    Set<Tag> tags = tagsByFeedEntryId.getOrDefault(entry.getId(), Set.of());
                    boolean isRead = readFeedEntryIds.contains(entry.getId());
                    long likeCount = likeCounts.getOrDefault(entry.getId(), 0L);
                    boolean isLiked = likedFeedEntryIds.contains(entry.getId());
                    return FeedEntryInfo.from(entry, blogName, siteUrl, tags, isRead, likeCount, isLiked);
                })
                .toList();

        return FeedEntryPage.of(content, hasMore, sortType);
    }

    /**
     * FeedEntryPage 생성 (LIKE_COUNT 정렬용 - 쿼리에서 likeCount 직접 계산)
     */
    private FeedEntryPage buildFeedEntryPageWithLikeCount(List<Tuple> results, FeedSearchCriteria criteria) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;

        boolean hasMore = results.size() > criteria.size();
        if (hasMore) {
            results = results.subList(0, criteria.size());
        }

        Set<Long> feedEntryIds = results.stream()
                .map(tuple -> tuple.get(feed))
                .filter(f -> f != null)
                .map(FeedEntry::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Tag>> tagsByFeedEntryId = fetchTagsByFeedEntryIds(feedEntryIds, criteria.memberId());
        Set<Long> readFeedEntryIds = fetchReadFeedEntryIds(feedEntryIds, criteria.memberId());
        Set<Long> likedFeedEntryIds = fetchLikedFeedEntryIds(feedEntryIds, criteria.memberId());

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> {
                    FeedEntry entry = tuple.get(feed);
                    String blogName = tuple.get(rss.blogName);
                    String siteUrl = tuple.get(rss.siteUrl);
                    Long likeCount = tuple.get(3, Long.class);
                    Set<Tag> tags = tagsByFeedEntryId.getOrDefault(entry.getId(), Set.of());
                    boolean isRead = readFeedEntryIds.contains(entry.getId());
                    boolean isLiked = likedFeedEntryIds.contains(entry.getId());
                    return FeedEntryInfo.from(entry, blogName, siteUrl, tags, isRead, likeCount != null ? likeCount : 0L, isLiked);
                })
                .toList();

        return FeedEntryPage.of(content, hasMore, FeedSortType.LIKE_COUNT);
    }

    /**
     * FeedEntryPage 생성 (LIKED_AT 정렬용 - likedAt 포함)
     */
    private FeedEntryPage buildFeedEntryPageWithLikedAt(List<Tuple> results, FeedSearchCriteria criteria) {
        QFeedEntry feed = feedEntry;
        QRssInfo rss = rssInfo;
        QFeedLike like = feedLike;

        boolean hasMore = results.size() > criteria.size();
        if (hasMore) {
            results = results.subList(0, criteria.size());
        }

        Set<Long> feedEntryIds = results.stream()
                .map(tuple -> tuple.get(feed))
                .filter(f -> f != null)
                .map(FeedEntry::getId)
                .collect(Collectors.toSet());

        Map<Long, Set<Tag>> tagsByFeedEntryId = fetchTagsByFeedEntryIds(feedEntryIds, criteria.memberId());
        Set<Long> readFeedEntryIds = fetchReadFeedEntryIds(feedEntryIds, criteria.memberId());
        Map<Long, Long> likeCounts = fetchLikeCounts(feedEntryIds);

        List<FeedEntryInfo> content = results.stream()
                .map(tuple -> {
                    FeedEntry entry = tuple.get(feed);
                    String blogName = tuple.get(rss.blogName);
                    String siteUrl = tuple.get(rss.siteUrl);
                    Instant likedAt = tuple.get(like.createdAt);
                    Set<Tag> tags = tagsByFeedEntryId.getOrDefault(entry.getId(), Set.of());
                    boolean isRead = readFeedEntryIds.contains(entry.getId());
                    long likeCount = likeCounts.getOrDefault(entry.getId(), 0L);
                    return FeedEntryInfo.from(entry, blogName, siteUrl, tags, isRead, likeCount, true, likedAt);
                })
                .toList();

        return FeedEntryPage.of(content, hasMore, FeedSortType.LIKED_AT);
    }

    private Set<Long> fetchReadFeedEntryIds(Set<Long> feedEntryIds, Long memberId) {
        if (feedEntryIds.isEmpty() || memberId == null) {
            return Set.of();
        }
        return memberFeedReadRepository.findReadFeedEntryIdsByMemberIdAndFeedEntryIdIn(memberId, feedEntryIds);
    }

    private Map<Long, Set<Tag>> fetchTagsByFeedEntryIds(Set<Long> feedEntryIds, Long memberId) {
        if (feedEntryIds.isEmpty()) {
            return Map.of();
        }

        if (memberId == null) {
            return feedEntryIds.stream()
                    .collect(Collectors.toMap(id -> id, id -> Set.of()));
        }

        List<FeedEntry> feedEntries = queryFactory
                .selectFrom(feedEntry)
                .leftJoin(feedEntry.tags, tag).fetchJoin()
                .where(feedEntry.id.in(feedEntryIds)
                        .and(tag.memberId.eq(memberId).or(tag.isNull())))
                .fetch();

        Map<Long, Set<Tag>> result = feedEntries.stream()
                .collect(Collectors.toMap(
                        FeedEntry::getId,
                        f -> f.getTags().stream()
                                .filter(t -> memberId.equals(t.getMemberId()))
                                .collect(Collectors.toSet()),
                        (existing, replacement) -> {
                            existing.addAll(replacement);
                            return existing;
                        }
                ));

        for (Long id : feedEntryIds) {
            result.putIfAbsent(id, Set.of());
        }

        return result;
    }

    private Map<Long, Long> fetchLikeCounts(Set<Long> feedEntryIds) {
        if (feedEntryIds.isEmpty()) {
            return Map.of();
        }

        Set<Object[]> results = feedLikeRepository.countByFeedEntryIdIn(feedEntryIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private Set<Long> fetchLikedFeedEntryIds(Set<Long> feedEntryIds, Long memberId) {
        if (feedEntryIds.isEmpty() || memberId == null) {
            return Set.of();
        }
        return feedLikeRepository.findLikedFeedEntryIdsByMemberIdAndFeedEntryIdIn(memberId, feedEntryIds);
    }
}
