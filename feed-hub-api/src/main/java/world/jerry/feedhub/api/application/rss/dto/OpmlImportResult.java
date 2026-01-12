package world.jerry.feedhub.api.application.rss.dto;

import java.util.List;

public record OpmlImportResult(
        int totalFound,
        int imported,
        List<String> skippedUrls,
        List<SyncResult> syncResults
) {
}
