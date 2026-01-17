package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;

import java.util.List;

public record RegisterRssSourceRequest(
        @NotBlank(message = "Blog name is required")
        @Size(max = 255, message = "Blog name must be at most 255 characters")
        String blogName,

        @Size(max = 255, message = "Author must be at most 255 characters")
        String author,

        @NotBlank(message = "RSS URL is required")
        @Size(max = 2048, message = "RSS URL must be at most 2048 characters")
        String rssUrl,

        @Size(max = 2048, message = "Site URL must be at most 2048 characters")
        String siteUrl,

        @Size(max = 10, message = "Language must be at most 10 characters")
        String language,

        List<Long> tagIds
) {
    public RegisterRssInfoCommand toCommand() {
        return new RegisterRssInfoCommand(blogName, author, rssUrl, siteUrl, language);
    }
}
