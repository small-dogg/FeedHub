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
        if (tagRepository.existsByMemberIdAndName(command.memberId(), command.name())) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다: " + command.name());
        }
        Tag tag = new Tag(command.memberId(), command.name());
        Tag saved = tagRepository.save(tag);
        return TagInfo.from(saved);
    }

    public TagInfo getTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("태그를 찾을 수 없습니다: " + id));
        return TagInfo.from(tag);
    }

    public List<TagInfo> getAllTagsByMember(Long memberId) {
        return tagRepository.findAllByMemberId(memberId).stream()
                .map(TagInfo::from)
                .toList();
    }

    @Transactional
    public void deleteTag(Long memberId, Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NoSuchElementException("태그를 찾을 수 없습니다: " + tagId));

        // 본인의 태그만 삭제 가능
        if (!tag.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 태그만 삭제할 수 있습니다.");
        }

        tagRepository.deleteById(tagId);
    }
}
