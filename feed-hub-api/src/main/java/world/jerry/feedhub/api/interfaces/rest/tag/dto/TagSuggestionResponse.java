package world.jerry.feedhub.api.interfaces.rest.tag.dto;

import world.jerry.feedhub.api.application.tag.dto.TagSuggestionInfo;

/**
 * 추천 태그 API 응답 DTO
 */
public record TagSuggestionResponse(String name, long userCount) {

    public static TagSuggestionResponse from(TagSuggestionInfo info) {
        return new TagSuggestionResponse(info.name(), info.userCount());
    }
}
