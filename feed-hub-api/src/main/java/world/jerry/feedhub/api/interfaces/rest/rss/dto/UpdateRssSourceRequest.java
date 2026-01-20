package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import jakarta.validation.constraints.Size;
import world.jerry.feedhub.api.domain.rss.BlogType;

public record UpdateRssSourceRequest(
        @Size(max = 255, message = "Blog name must be at most 255 characters")
        String blogName,

        @Size(max = 255, message = "Author must be at most 255 characters")
        String author,

        @Size(max = 2048, message = "Site URL must be at most 2048 characters")
        String siteUrl,

        @Size(max = 2048, message = "Crawl URL must be at most 2048 characters")
        String crawlUrl,

        @Size(max = 10, message = "Language must be at most 10 characters")
        String language,

        BlogType blogType
) {
}
