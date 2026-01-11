package world.jerry.feedhub.api.application.tag;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.tag.dto.CreateTagCommand;
import world.jerry.feedhub.api.application.tag.dto.TagInfo;
import world.jerry.feedhub.api.domain.tag.Tag;
import world.jerry.feedhub.api.domain.tag.TagRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public TagInfo createTag(CreateTagCommand command) {
        if (tagRepository.existsByName(command.name())) {
            throw new IllegalArgumentException("Tag with name '" + command.name() + "' already exists");
        }
        Tag tag = new Tag(command.name());
        Tag saved = tagRepository.save(tag);
        return TagInfo.from(saved);
    }

    public TagInfo getTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Tag not found with id: " + id));
        return TagInfo.from(tag);
    }

    public List<TagInfo> getAllTags() {
        return tagRepository.findAll().stream()
                .map(TagInfo::from)
                .toList();
    }

    @Transactional
    public void deleteTag(Long id) {
        if (tagRepository.findById(id).isEmpty()) {
            throw new NoSuchElementException("Tag not found with id: " + id);
        }
        tagRepository.deleteById(id);
    }
}
