package world.jerry.feedhub.api.domain.qna;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QnaRepository extends JpaRepository<Qna, Long> {

    /**
     * 특정 회원의 QnA 목록 조회 (최신순)
     */
    List<Qna> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 전체 QnA 목록 조회 (최신순) - 관리자용
     */
    List<Qna> findAllByOrderByCreatedAtDesc();
}
