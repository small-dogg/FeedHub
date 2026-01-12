package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.OpmlImportResult;

import java.util.List;

public record OpmlImportResponse(
        int totalFound,
        int imported,
        int skipped,
        List<String> skippedUrls,
        List<SyncResponse> syncResults
) {
    public static OpmlImportResponse from(OpmlImportResult result) {
        List<SyncResponse> syncResponses = result.syncResults().stream()
                .map(SyncResponse::from)
                .toList();

        return new OpmlImportResponse(
                result.totalFound(),
                result.imported(),
                result.skippedUrls().size(),
                result.skippedUrls(),
                syncResponses
        );
    }
}
