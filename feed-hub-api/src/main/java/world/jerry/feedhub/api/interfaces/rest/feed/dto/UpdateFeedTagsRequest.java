package world.jerry.feedhub.api.interfaces.rest.feed.dto;

import world.jerry.feedhub.api.application.feed.dto.UpdateFeedTagsCommand;

import java.util.List;

/**
 * 피드 태그 업데이트 요청
 * @param tagIds 기존 태그 ID 목록
 * @param newTagNames 새로 생성할 태그명 목록
 */
public record UpdateFeedTagsRequest(
        List<Long> tagIds,
        List<String> newTagNames
) {
    public UpdateFeedTagsCommand toCommand() {
        return new UpdateFeedTagsCommand(tagIds, newTagNames);
    }
}
