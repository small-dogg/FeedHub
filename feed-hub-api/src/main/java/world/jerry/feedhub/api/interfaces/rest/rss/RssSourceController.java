package world.jerry.feedhub.api.interfaces.rest.rss;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import world.jerry.feedhub.api.application.rss.OpmlImportService;
import world.jerry.feedhub.api.application.rss.RssInfoService;
import world.jerry.feedhub.api.application.rss.RssSyncService;
import world.jerry.feedhub.api.application.rss.dto.OpmlImportResult;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.OpmlImportResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.RegisterRssSourceRequest;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.RssSourceResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.SyncResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.UpdateTagsRequest;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rss-sources")
@RequiredArgsConstructor
public class RssSourceController {

    private final RssInfoService rssInfoService;
    private final RssSyncService rssSyncService;
    private final OpmlImportService opmlImportService;

    @PostMapping
    public ResponseEntity<RssSourceResponse> registerRssSource(@Valid @RequestBody RegisterRssSourceRequest request) {
        RssInfoDetail detail = rssInfoService.registerRssInfo(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(RssSourceResponse.from(detail));
    }

    @GetMapping
    public ResponseEntity<List<RssSourceResponse>> getAllRssSources() {
        List<RssSourceResponse> sources = rssInfoService.getAllRssInfos().stream()
                .map(RssSourceResponse::from)
                .toList();
        return ResponseEntity.ok(sources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RssSourceResponse> getRssSource(@PathVariable Long id) {
        RssInfoDetail detail = rssInfoService.getRssInfo(id);
        return ResponseEntity.ok(RssSourceResponse.from(detail));
    }

    @PutMapping("/{id}/tags")
    public ResponseEntity<RssSourceResponse> updateTags(@PathVariable Long id, @RequestBody UpdateTagsRequest request) {
        RssInfoDetail detail = rssInfoService.updateTags(id, request.toCommand());
        return ResponseEntity.ok(RssSourceResponse.from(detail));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregisterRssSource(@PathVariable Long id) {
        rssInfoService.unregisterRssInfo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<SyncResponse> syncRssSource(@PathVariable Long id) {
        return ResponseEntity.ok(SyncResponse.from(rssSyncService.syncRssSource(id)));
    }

    @PostMapping("/sync-all")
    public ResponseEntity<List<SyncResponse>> syncAllRssSources() {
        List<SyncResponse> results = rssSyncService.syncAllRssSources().stream()
                .map(SyncResponse::from)
                .toList();
        return ResponseEntity.ok(results);
    }

    @PostMapping("/import/opml")
    public ResponseEntity<OpmlImportResponse> importOpml(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "syncAfterImport", defaultValue = "true") boolean syncAfterImport
    ) throws IOException {
        OpmlImportResult result = opmlImportService.importOpml(file.getInputStream(), syncAfterImport);
        return ResponseEntity.ok(OpmlImportResponse.from(result));
    }
}
