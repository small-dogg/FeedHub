package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.UpdateTagsCommand;

import java.util.List;

public record UpdateTagsRequest(List<Long> tagIds) {

    public UpdateTagsCommand toCommand() {
        return new UpdateTagsCommand(tagIds);
    }
}
