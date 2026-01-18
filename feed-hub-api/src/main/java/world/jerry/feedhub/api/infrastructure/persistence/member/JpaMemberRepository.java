package world.jerry.feedhub.api.infrastructure.persistence.member;

import org.springframework.data.jpa.repository.JpaRepository;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;

import java.util.Optional;

public interface JpaMemberRepository extends JpaRepository<Member, Long>, MemberRepository {

    @Override
    Optional<Member> findByEmail(String email);

    @Override
    boolean existsByEmail(String email);
}
