package world.jerry.feedhub.api.interfaces.rest.tag.dto;

import world.jerry.feedhub.api.application.tag.dto.TagInfo;

import java.time.Instant;

public record TagResponse(Long id, String name, Instant createdAt) {

    public static TagResponse from(TagInfo info) {
        return new TagResponse(info.id(), info.name(), info.createdAt());
    }
}
