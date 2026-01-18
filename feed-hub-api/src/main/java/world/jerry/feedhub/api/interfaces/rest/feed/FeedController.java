package world.jerry.feedhub.api.interfaces.rest.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.feed.FeedEntryService;
import world.jerry.feedhub.api.application.feed.FeedQueryService;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryInfo;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;
import world.jerry.feedhub.api.application.feed.dto.FeedSearchCriteria;
import world.jerry.feedhub.api.interfaces.rest.feed.dto.FeedEntryResponse;
import world.jerry.feedhub.api.interfaces.rest.feed.dto.FeedPageResponse;
import world.jerry.feedhub.api.interfaces.rest.feed.dto.UpdateFeedTagsRequest;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedQueryService feedQueryService;
    private final FeedEntryService feedEntryService;

    @GetMapping
    public ResponseEntity<FeedPageResponse> searchFeeds(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) List<Long> rssSourceIds,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false) Instant lastPublishedAt,
            @RequestParam(defaultValue = "20") int size
    ) {
        FeedSearchCriteria criteria = new FeedSearchCriteria(memberId, rssSourceIds, tagIds, query, lastId, lastPublishedAt, size);
        FeedEntryPage feedPage = feedQueryService.searchFeeds(criteria);
        return ResponseEntity.ok(FeedPageResponse.from(feedPage));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<FeedEntryResponse> updateTags(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestBody UpdateFeedTagsRequest request
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        FeedEntryInfo feedEntryInfo = feedEntryService.updateTags(id, request.toCommand(memberId));
        return ResponseEntity.ok(FeedEntryResponse.from(feedEntryInfo));
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long id) {
        feedEntryService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
