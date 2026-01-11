package world.jerry.feedhub.api.application.rss;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.rss.dto.RegisterRssInfoCommand;
import world.jerry.feedhub.api.application.rss.dto.RssInfoDetail;
import world.jerry.feedhub.api.application.rss.dto.UpdateTagsCommand;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.domain.tag.Tag;
import world.jerry.feedhub.api.domain.tag.TagRepository;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RssInfoService {

    private final RssInfoRepository rssInfoRepository;
    private final TagRepository tagRepository;

    @Transactional
    public RssInfoDetail registerRssInfo(RegisterRssInfoCommand command) {
        if (rssInfoRepository.existsByRssUrl(command.rssUrl())) {
            throw new IllegalArgumentException("RSS source with URL '" + command.rssUrl() + "' already exists");
        }

        RssInfo rssInfo = new RssInfo(
                command.blogName(),
                command.author(),
                command.rssUrl(),
                command.siteUrl(),
                command.language()
        );

        if (command.tagIds() != null && !command.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllByIdIn(command.tagIds());
            rssInfo.updateTags(new HashSet<>(tags));
        }

        RssInfo saved = rssInfoRepository.save(rssInfo);
        return RssInfoDetail.from(saved);
    }

    public RssInfoDetail getRssInfo(Long id) {
        RssInfo rssInfo = rssInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RSS source not found with id: " + id));
        return RssInfoDetail.from(rssInfo);
    }

    public List<RssInfoDetail> getAllRssInfos() {
        return rssInfoRepository.findAll().stream()
                .map(RssInfoDetail::from)
                .toList();
    }

    @Transactional
    public RssInfoDetail updateTags(Long id, UpdateTagsCommand command) {
        RssInfo rssInfo = rssInfoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RSS source not found with id: " + id));

        Set<Tag> newTags = new HashSet<>();
        if (command.tagIds() != null && !command.tagIds().isEmpty()) {
            newTags.addAll(tagRepository.findAllByIdIn(command.tagIds()));
        }
        rssInfo.updateTags(newTags);

        return RssInfoDetail.from(rssInfo);
    }

    @Transactional
    public void unregisterRssInfo(Long id) {
        if (rssInfoRepository.findById(id).isEmpty()) {
            throw new NoSuchElementException("RSS source not found with id: " + id);
        }
        rssInfoRepository.deleteById(id);
    }
}
