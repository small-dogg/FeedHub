package world.jerry.feedhub.api.application.rss.dto;

import java.util.List;

public record UpdateTagsCommand(List<Long> tagIds) {
}
