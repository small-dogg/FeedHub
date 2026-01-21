package world.jerry.feedhub.api.interfaces.rest.rss.dto;

import world.jerry.feedhub.api.application.rss.dto.OpmlImportResult;

import java.util.List;

public record OpmlImportResponse(
        int totalFound,
        int imported,
        int skipped,
        List<String> skippedUrls,
        List<Long> syncRequestedIds,
        String message
) {
    public static OpmlImportResponse from(OpmlImportResult result) {
        return new OpmlImportResponse(
                result.totalFound(),
                result.imported(),
                result.skippedUrls().size(),
                result.skippedUrls(),
                result.syncRequestedIds(),
                "OPML 임포트 완료. 동기화는 비동기로 처리됩니다."
        );
    }
}
