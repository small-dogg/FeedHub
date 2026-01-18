package world.jerry.feedhub.api.application.feed.dto;

import world.jerry.feedhub.api.domain.feed.FeedEntry;
import world.jerry.feedhub.api.domain.tag.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record FeedEntryInfo(
        Long id,
        Long rssInfoId,
        String rssInfoBlogName,
        String rssInfoSiteUrl,
        String title,
        String link,
        String description,
        String author,
        Instant publishedAt,
        Long viewCount,
        List<TagSummary> tags
) {
    public static FeedEntryInfo from(FeedEntry entry, String blogName, String siteUrl, Set<Tag> tags) {
        List<TagSummary> tagSummaries = tags != null
                ? tags.stream().map(TagSummary::from).toList()
                : List.of();
        return new FeedEntryInfo(
                entry.getId(),
                entry.getRssInfoId(),
                blogName,
                siteUrl,
                entry.getTitle(),
                entry.getLink(),
                entry.getDescription(),
                entry.getAuthor(),
                entry.getPublishedAt(),
                entry.getViewCount(),
                tagSummaries
        );
    }

    public record TagSummary(Long id, String name) {
        public static TagSummary from(Tag tag) {
            return new TagSummary(tag.getId(), tag.getName());
        }
    }
}
