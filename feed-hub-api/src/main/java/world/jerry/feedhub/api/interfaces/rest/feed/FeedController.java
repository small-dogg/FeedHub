package world.jerry.feedhub.api.interfaces.rest.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.feed.FeedQueryService;
import world.jerry.feedhub.api.application.feed.dto.FeedEntryPage;
import world.jerry.feedhub.api.application.feed.dto.FeedSearchCriteria;
import world.jerry.feedhub.api.interfaces.rest.feed.dto.FeedPageResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedQueryService feedQueryService;

    @GetMapping
    public ResponseEntity<FeedPageResponse> searchFeeds(
            @RequestParam(required = false) List<Long> rssSourceIds,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        FeedSearchCriteria criteria = new FeedSearchCriteria(rssSourceIds, tagIds, page, size);
        FeedEntryPage feedPage = feedQueryService.searchFeeds(criteria);
        return ResponseEntity.ok(FeedPageResponse.from(feedPage));
    }
}
