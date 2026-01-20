package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;

public record RegisterRssSourceRequest(
        @NotBlank(message = "RSS URL is required")
        @Size(max = 2048, message = "RSS URL must be at most 2048 characters")
        String rssUrl,

        @Size(max = 2048, message = "Crawl URL must be at most 2048 characters")
        String crawlUrl
) {
    public RegisterRssInfoCommand toCommand() {
        return new RegisterRssInfoCommand(null, null, rssUrl, null, crawlUrl, null, null);
    }
}
