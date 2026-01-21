package world.jerry.feedhub.api.application.tag.dto;

/**
 * 추천 태그 정보 DTO
 * - 다른 사용자들이 많이 사용하는 태그를 추천할 때 사용
 */
public record TagSuggestionInfo(String name, long userCount) {
}
