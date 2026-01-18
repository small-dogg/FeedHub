package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

/**
 * 피드 태그 업데이트 커맨드
 * @param memberId 태그를 관리하는 회원 ID
 * @param tagIds 기존 태그 ID 목록
 * @param newTagNames 새로 생성할 태그명 목록 (자동 생성)
 */
public record UpdateFeedTagsCommand(
        Long memberId,
        List<Long> tagIds,
        List<String> newTagNames
) {
}
