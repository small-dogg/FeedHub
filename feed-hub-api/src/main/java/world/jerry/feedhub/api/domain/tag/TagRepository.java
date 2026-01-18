package world.jerry.feedhub.api.domain.tag;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findById(Long id);

    Optional<Tag> findByMemberIdAndName(Long memberId, String name);

    List<Tag> findAllByMemberId(Long memberId);

    List<Tag> findAllByIdIn(List<Long> ids);

    List<Tag> findAllByMemberIdAndIdIn(Long memberId, List<Long> ids);

    void deleteById(Long id);

    boolean existsByMemberIdAndName(Long memberId, String name);
}
