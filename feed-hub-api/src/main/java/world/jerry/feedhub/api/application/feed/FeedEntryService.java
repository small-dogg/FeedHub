package world.jerry.feedhub.api.application.feed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryInfo;
import world.jerry.feedhub.api.application.feed.dto.UpdateFeedTagsCommand;
import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.feed.FeedEntryRepository;
import world.jerry.feedhub.api.domain.feed.FeedLike;
import world.jerry.feedhub.api.domain.feed.FeedLikeRepository;
import world.jerry.feedhub.api.domain.feed.MemberFeedRead;
import world.jerry.feedhub.api.domain.feed.MemberFeedReadRepository;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.domain.tag.Tag;
import world.jerry.feedhub.api.domain.tag.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * 피드 엔트리 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedEntryService {

    private final FeedEntryRepository feedEntryRepository;
    private final MemberFeedReadRepository memberFeedReadRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final RssInfoRepository rssInfoRepository;
    private final TagRepository tagRepository;

    /**
     * 피드 엔트리에 태그 업데이트
     * 존재하지 않는 태그명은 자동으로 생성 (해당 회원의 태그로)
     */
    @Transactional
    public FeedEntryInfo updateTags(Long feedEntryId, UpdateFeedTagsCommand command) {
        FeedEntry feedEntry = feedEntryRepository.findById(feedEntryId)
                .orElseThrow(() -> new NoSuchElementException("피드를 찾을 수 없습니다: " + feedEntryId));

        Long memberId = command.memberId();
        Set<Tag> tagsToAssign = new HashSet<>();

        // 기존 태그 ID로 태그 조회 (해당 회원의 태그만)
        if (command.tagIds() != null && !command.tagIds().isEmpty()) {
            List<Tag> existingTags = tagRepository.findAllByMemberIdAndIdIn(memberId, command.tagIds());
            tagsToAssign.addAll(existingTags);
        }

        // 새 태그명으로 태그 생성 또는 조회 (해당 회원의 태그)
        if (command.newTagNames() != null && !command.newTagNames().isEmpty()) {
            for (String tagName : command.newTagNames()) {
                String trimmedName = tagName.trim();
                if (trimmedName.isEmpty()) continue;

                Tag tag = tagRepository.findByMemberIdAndName(memberId, trimmedName)
                        .orElseGet(() -> {
                            log.info("새 태그 생성 - memberId: {}, name: {}", memberId, trimmedName);
                            return tagRepository.save(new Tag(memberId, trimmedName));
                        });
                tagsToAssign.add(tag);
            }
        }

        feedEntry.updateTags(tagsToAssign);

        // RSS 정보 조회 (블로그 이름, 사이트 URL)
        RssInfo rssInfo = rssInfoRepository.findById(feedEntry.getRssInfoId()).orElse(null);
        String blogName = rssInfo != null ? rssInfo.getBlogName() : null;
        String siteUrl = rssInfo != null ? rssInfo.getSiteUrl() : null;

        return FeedEntryInfo.from(feedEntry, blogName, siteUrl, feedEntry.getTags());
    }

    /**
     * 피드 조회수 증가 (원자적 업데이트로 동시성 안전)
     */
    @Transactional
    public void incrementViewCount(Long feedEntryId) {
        int updated = feedEntryRepository.incrementViewCount(feedEntryId);
        if (updated == 0) {
            log.warn("피드 조회수 증가 실패 - 존재하지 않는 피드: {}", feedEntryId);
        }
    }

    /**
     * 피드를 읽음 처리하고 조회수 증가
     * 이미 읽은 피드는 중복 처리하지 않음
     * @return true if newly marked as read, false if already read
     */
    @Transactional
    public boolean markAsRead(Long feedEntryId, Long memberId) {
        if (memberId == null) {
            // 비로그인 상태에서는 조회수만 증가
            incrementViewCount(feedEntryId);
            return false;
        }

        // 이미 읽은 피드인지 확인
        if (memberFeedReadRepository.existsByMemberIdAndFeedEntryId(memberId, feedEntryId)) {
            log.debug("이미 읽은 피드 - memberId: {}, feedEntryId: {}", memberId, feedEntryId);
            return false;
        }

        // 읽음 기록 저장
        memberFeedReadRepository.save(new MemberFeedRead(memberId, feedEntryId));

        // 조회수 증가
        incrementViewCount(feedEntryId);

        log.debug("피드 읽음 처리 완료 - memberId: {}, feedEntryId: {}", memberId, feedEntryId);
        return true;
    }

    /**
     * 피드 좋아요 토글
     * @return true if liked, false if unliked
     */
    @Transactional
    public boolean toggleLike(Long feedEntryId, Long memberId) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        // 피드 존재 확인
        if (!feedEntryRepository.existsById(feedEntryId)) {
            throw new NoSuchElementException("피드를 찾을 수 없습니다: " + feedEntryId);
        }

        // 이미 좋아요한 경우 취소
        if (feedLikeRepository.existsByMemberIdAndFeedEntryId(memberId, feedEntryId)) {
            feedLikeRepository.deleteByMemberIdAndFeedEntryId(memberId, feedEntryId);
            log.debug("피드 좋아요 취소 - memberId: {}, feedEntryId: {}", memberId, feedEntryId);
            return false;
        }

        // 좋아요 추가
        feedLikeRepository.save(new FeedLike(memberId, feedEntryId));
        log.debug("피드 좋아요 추가 - memberId: {}, feedEntryId: {}", memberId, feedEntryId);
        return true;
    }

    /**
     * 피드 좋아요 수 조회
     */
    public long getLikeCount(Long feedEntryId) {
        return feedLikeRepository.countByFeedEntryId(feedEntryId);
    }

    /**
     * 특정 회원이 피드를 좋아요했는지 확인
     */
    public boolean isLikedByMember(Long feedEntryId, Long memberId) {
        if (memberId == null) {
            return false;
        }
        return feedLikeRepository.existsByMemberIdAndFeedEntryId(memberId, feedEntryId);
    }
}
