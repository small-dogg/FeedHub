package world.jerry.feedhub.api.application.feed.dto;

import java.util.List;

/**
 * 피드 태그 업데이트 커맨드
 * @param tagIds 기존 태그 ID 목록
 * @param newTagNames 새로 생성할 태그명 목록 (자동 생성)
 */
public record UpdateFeedTagsCommand(
        List<Long> tagIds,
        List<String> newTagNames
) {
}
