package world.jerry.feedhub.api.infrastructure.persistence.tag;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.api.domain.tag.Tag;
import world.jerry.feedhub.api.domain.tag.TagRepository;

import java.util.List;
import java.util.Optional;

public interface JpaTagRepository extends JpaRepository<Tag, Long>, TagRepository {

    @Override
    Optional<Tag> findByMemberIdAndName(Long memberId, String name);

    @Override
    List<Tag> findAllByMemberId(Long memberId);

    @Override
    List<Tag> findAllByIdIn(List<Long> ids);

    @Override
    List<Tag> findAllByMemberIdAndIdIn(Long memberId, List<Long> ids);

    @Override
    boolean existsByMemberIdAndName(Long memberId, String name);
}
