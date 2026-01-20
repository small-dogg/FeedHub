package world.jerry.feedhub.api.domain.qna;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /**
     * 특정 QnA의 답변 목록 조회 (작성순)
     */
    List<QnaAnswer> findByQnaIdOrderByCreatedAtAsc(Long qnaId);

    /**
     * 특정 QnA의 답변 수 조회
     */
    long countByQnaId(Long qnaId);

    /**
     * 여러 QnA의 답변 수를 한 번에 조회
     */
    @Query("SELECT a.qnaId, COUNT(a) FROM QnaAnswer a WHERE a.qnaId IN :qnaIds GROUP BY a.qnaId")
    Set<Object[]> countByQnaIdIn(@Param("qnaIds") Set<Long> qnaIds);
}
