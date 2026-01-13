package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.infrastructure.opml.OpmlBuilder;

import java.util.List;

/**
 * OPML 내보내기 서비스
 * 등록된 RSS 소스를 OPML 포맷으로 내보내기
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpmlExportService {

    private final RssInfoRepository rssInfoRepository;
    private final OpmlBuilder opmlBuilder;

    /**
     * 전체 RSS 소스를 OPML 문서로 내보내기
     */
    public String exportAll() {
        log.info("OPML 전체 내보내기 시작");

        List<RssInfoDetail> rssInfos = rssInfoRepository.findAll().stream()
                .map(RssInfoDetail::from)
                .toList();

        log.info("OPML 내보내기 대상: {}개 RSS 소스", rssInfos.size());
        return opmlBuilder.build(rssInfos);
    }
}
