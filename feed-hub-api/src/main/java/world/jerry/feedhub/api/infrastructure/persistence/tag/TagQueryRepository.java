package world.jerry.feedhub.api.infrastructure.persistence.tag;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import world.jerry.feedhub.api.application.tag.dto.TagSuggestionInfo;

import java.util.List;

import static world.jerry.feedhub.api.domain.feed.QFeedEntry.feedEntry;
import static world.jerry.feedhub.api.domain.tag.QTag.tag;

/**
 * 태그 조회 전용 QueryDSL Repository
 */
@Repository
@RequiredArgsConstructor
public class TagQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 특정 피드에 대한 추천 태그 조회
     * - 해당 피드에 다른 사용자들이 붙여놓은 태그를 조회
     * - 현재 사용자가 이미 해당 피드에 붙인 태그는 제외
     * - 사용자 수 기준 내림차순 정렬
     */
    public List<TagSuggestionInfo> findSuggestionsForFeed(Long feedEntryId, Long memberId, int limit) {
        return queryFactory
                .select(Projections.constructor(TagSuggestionInfo.class,
                        tag.name,
                        tag.memberId.countDistinct()))
                .from(feedEntry)
                .join(feedEntry.tags, tag)
                .where(feedEntry.id.eq(feedEntryId)
                        .and(tag.memberId.ne(memberId)))
                .groupBy(tag.name)
                .orderBy(tag.memberId.countDistinct().desc())
                .limit(limit)
                .fetch();
    }
}
