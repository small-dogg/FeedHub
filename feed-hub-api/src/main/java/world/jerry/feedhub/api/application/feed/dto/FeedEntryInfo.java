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
        List<TagSummary> tags,
        boolean isRead,
        long likeCount,
        boolean isLiked
) {
    public static FeedEntryInfo from(FeedEntry entry, String blogName, String siteUrl, Set<Tag> tags) {
        return from(entry, blogName, siteUrl, tags, false, 0L, false);
    }

    public static FeedEntryInfo from(FeedEntry entry, String blogName, String siteUrl, Set<Tag> tags, boolean isRead) {
        return from(entry, blogName, siteUrl, tags, isRead, 0L, false);
    }

    public static FeedEntryInfo from(FeedEntry entry, String blogName, String siteUrl, Set<Tag> tags,
                                     boolean isRead, long likeCount, boolean isLiked) {
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
                tagSummaries,
                isRead,
                likeCount,
                isLiked
        );
    }

    public record TagSummary(Long id, String name) {
        public static TagSummary from(Tag tag) {
            return new TagSummary(tag.getId(), tag.getName());
        }
    }
}
