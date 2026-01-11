package world.jerry.feedhub.api.domain.tag;

import java.util.List;
import java.util.Optional;

public interface TagRepository {

    Tag save(Tag tag);

    Optional<Tag> findById(Long id);

    Optional<Tag> findByName(String name);

    List<Tag> findAll();

    List<Tag> findAllByIdIn(List<Long> ids);

    void deleteById(Long id);

    boolean existsByName(String name);
}
