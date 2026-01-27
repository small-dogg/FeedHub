package world.jerry.feedhub.api.interfaces.rest.rss;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import world.jerry.feedhub.api.application.rss.CollectorCommandService;
import world.jerry.feedhub.api.application.rss.CrawlService;
import world.jerry.feedhub.api.application.rss.OpmlExportService;
import world.jerry.feedhub.api.application.rss.OpmlImportService;
import world.jerry.feedhub.api.application.rss.RssInfoService;
import world.jerry.feedhub.api.application.rss.dto.OpmlImportResult;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.common.domain.CrawlMode;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.AsyncCommandResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.CrawlResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.OpmlImportResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.RegisterRssSourceRequest;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.RssSourceResponse;
import world.jerry.feedhub.api.interfaces.rest.rss.dto.UpdateRssSourceRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rss-sources")
@RequiredArgsConstructor
public class RssSourceController {

    private final RssInfoService rssInfoService;
    private final CollectorCommandService collectorCommandService;
    private final CrawlService crawlService;
    private final OpmlImportService opmlImportService;
    private final OpmlExportService opmlExportService;

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregisterRssSource(@PathVariable Long id) {
        rssInfoService.unregisterRssInfo(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<AsyncCommandResponse> syncRssSource(@PathVariable Long id) {
        String commandId = collectorCommandService.requestSync(id, "api:user");
        return ResponseEntity.accepted().body(AsyncCommandResponse.syncAccepted(commandId));
    }

    @PostMapping("/sync-all")
    public ResponseEntity<AsyncCommandResponse> syncAllRssSources() {
        collectorCommandService.requestSyncAll("api:user");
        return ResponseEntity.accepted().body(AsyncCommandResponse.syncAllAccepted("batch"));
    }

    @PostMapping("/{id}/crawl")
    public ResponseEntity<CrawlResponse> crawlRssSource(
            @PathVariable Long id,
            @RequestParam(value = "mode", defaultValue = "FULL") CrawlMode mode) {
        return ResponseEntity.ok(CrawlResponse.from(crawlService.requestCrawl(id, mode)));
    }

    @PostMapping("/crawl-all")
    public ResponseEntity<List<CrawlResponse>> crawlAllRssSources(
            @RequestParam(value = "mode", defaultValue = "FULL") CrawlMode mode) {
        List<CrawlResponse> results = rssInfoService.getAllRssInfos().stream()
                .map(info -> CrawlResponse.from(crawlService.requestCrawl(info.id(), mode)))
                .toList();
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RssSourceResponse> updateRssSource(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRssSourceRequest request) {
        RssInfoDetail detail = rssInfoService.updateRssInfo(
                id,
                request.blogName(),
                request.author(),
                request.siteUrl(),
                request.crawlUrl(),
                request.language(),
                request.blogType()
        );
        return ResponseEntity.ok(RssSourceResponse.from(detail));
    }

    @PostMapping("/import/opml")
    public ResponseEntity<OpmlImportResponse> importOpml(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "syncAfterImport", defaultValue = "true") boolean syncAfterImport
    ) throws IOException {
        OpmlImportResult result = opmlImportService.importOpml(file.getInputStream(), syncAfterImport);
        return ResponseEntity.ok(OpmlImportResponse.from(result));
    }

    @GetMapping("/export/opml")
    public ResponseEntity<byte[]> exportOpml() {
        String opmlContent = opmlExportService.exportAll();
        byte[] contentBytes = opmlContent.getBytes(StandardCharsets.UTF_8);

        String filename = "feedhub-subscriptions-" +
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".opml";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok().headers(headers).body(contentBytes);
    }
}
