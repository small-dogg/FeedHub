package world.jerry.feedhub.api.application.tag.dto;

import world.jerry.feedhub.api.domain.tag.Tag;

import java.time.Instant;

public record TagInfo(Long id, String name, Instant createdAt) {

    public static TagInfo from(Tag tag) {
        return new TagInfo(tag.getId(), tag.getName(), tag.getCreatedAt());
    }
}
