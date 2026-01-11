package world.jerry.feedhub.api.application.rss.dto;

import world.jerry.feedhub.api.application.tag.dto.TagInfo;
import world.jerry.feedhub.api.domain.rss.RssInfo;

import java.time.Instant;
import java.util.List;

public record RssInfoDetail(
        Long id,
        String blogName,
        String author,
        String rssUrl,
        String siteUrl,
        String language,
        Instant createdAt,
        Instant lastSyncAt,
        List<TagInfo> tags
) {
    public static RssInfoDetail from(RssInfo rssInfo) {
        List<TagInfo> tagInfos = rssInfo.getTags().stream()
                .map(TagInfo::from)
                .toList();
        return new RssInfoDetail(
                rssInfo.getId(),
                rssInfo.getBlogName(),
                rssInfo.getAuthor(),
                rssInfo.getRssUrl(),
                rssInfo.getSiteUrl(),
                rssInfo.getLanguage(),
                rssInfo.getCreatedAt(),
                rssInfo.getLastSyncAt(),
                tagInfos
        );
    }
}
