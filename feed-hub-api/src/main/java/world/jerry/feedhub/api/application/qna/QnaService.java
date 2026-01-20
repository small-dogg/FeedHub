package world.jerry.feedhub.api.application.qna;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.qna.dto.*;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;
import world.jerry.feedhub.api.domain.qna.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * QnA 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QnaRepository qnaRepository;
    private final QnaAnswerRepository qnaAnswerRepository;
    private final MemberRepository memberRepository;

    /**
     * QnA 질문 등록
     */
    @Transactional
    public QnaInfo createQuestion(CreateQnaCommand command) {
        Qna qna = new Qna(
                command.memberId(),
                command.type(),
                command.title(),
                command.content(),
                command.isSecret()
        );
        Qna saved = qnaRepository.save(qna);
        log.info("QnA 등록 - id: {}, memberId: {}, type: {}", saved.getId(), command.memberId(), command.type());

        String nickname = getMemberNickname(command.memberId());
        return QnaInfo.from(saved, nickname, 0);
    }

    /**
     * 내 QnA 목록 조회
     */
    public List<QnaInfo> getMyQuestions(Long memberId) {
        List<Qna> qnaList = qnaRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return toQnaInfoList(qnaList);
    }

    /**
     * 전체 QnA 목록 조회 (관리자용)
     */
    public List<QnaInfo> getAllQuestions() {
        List<Qna> qnaList = qnaRepository.findAllByOrderByCreatedAtDesc();
        return toQnaInfoList(qnaList);
    }

    /**
     * QnA 상세 조회
     * 비밀글은 작성자 본인 또는 관리자만 조회 가능
     */
    public QnaDetailInfo getQuestion(Long qnaId, Long requestMemberId, boolean isAdmin) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NoSuchElementException("QnA를 찾을 수 없습니다: " + qnaId));

        // 비밀글 접근 권한 체크
        if (qna.isSecret() && !qna.isOwnedBy(requestMemberId) && !isAdmin) {
            throw new IllegalAccessError("비밀글은 작성자 본인만 조회할 수 있습니다.");
        }

        String memberNickname = getMemberNickname(qna.getMemberId());
        List<QnaAnswerInfo> answers = getAnswersForQna(qnaId);

        return QnaDetailInfo.from(qna, memberNickname, answers);
    }

    /**
     * QnA 답변 등록 (관리자만)
     */
    @Transactional
    public QnaAnswerInfo createAnswer(Long qnaId, CreateAnswerCommand command) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NoSuchElementException("QnA를 찾을 수 없습니다: " + qnaId));

        QnaAnswer answer = new QnaAnswer(qnaId, command.memberId(), command.content());
        QnaAnswer saved = qnaAnswerRepository.save(answer);

        // QnA 상태를 '답변완료'로 변경
        qna.markAsAnswered();

        log.info("QnA 답변 등록 - qnaId: {}, answerId: {}, memberId: {}", qnaId, saved.getId(), command.memberId());

        String nickname = getMemberNickname(command.memberId());
        return QnaAnswerInfo.from(saved, nickname);
    }

    /**
     * QnA 삭제 (본인만)
     */
    @Transactional
    public void deleteQuestion(Long qnaId, Long memberId) {
        Qna qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new NoSuchElementException("QnA를 찾을 수 없습니다: " + qnaId));

        if (!qna.isOwnedBy(memberId)) {
            throw new IllegalAccessError("본인의 QnA만 삭제할 수 있습니다.");
        }

        qnaRepository.delete(qna);
        log.info("QnA 삭제 - qnaId: {}, memberId: {}", qnaId, memberId);
    }

    // === Private Helper Methods ===

    private List<QnaInfo> toQnaInfoList(List<Qna> qnaList) {
        if (qnaList.isEmpty()) {
            return List.of();
        }

        // 회원 정보 일괄 조회
        Set<Long> memberIds = qnaList.stream()
                .map(Qna::getMemberId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = getNicknameMap(memberIds);

        // 답변 수 일괄 조회
        Set<Long> qnaIds = qnaList.stream()
                .map(Qna::getId)
                .collect(Collectors.toSet());
        Map<Long, Long> answerCountMap = getAnswerCountMap(qnaIds);

        return qnaList.stream()
                .map(qna -> QnaInfo.from(
                        qna,
                        nicknameMap.getOrDefault(qna.getMemberId(), "알 수 없음"),
                        answerCountMap.getOrDefault(qna.getId(), 0L)
                ))
                .toList();
    }

    private List<QnaAnswerInfo> getAnswersForQna(Long qnaId) {
        List<QnaAnswer> answers = qnaAnswerRepository.findByQnaIdOrderByCreatedAtAsc(qnaId);
        if (answers.isEmpty()) {
            return List.of();
        }

        Set<Long> memberIds = answers.stream()
                .map(QnaAnswer::getMemberId)
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = getNicknameMap(memberIds);

        return answers.stream()
                .map(answer -> QnaAnswerInfo.from(
                        answer,
                        nicknameMap.getOrDefault(answer.getMemberId(), "알 수 없음")
                ))
                .toList();
    }

    private String getMemberNickname(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::getNickname)
                .orElse("알 수 없음");
    }

    private Map<Long, String> getNicknameMap(Set<Long> memberIds) {
        // 개별 조회 (추후 batch 조회로 최적화 가능)
        return memberIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        this::getMemberNickname
                ));
    }

    private Map<Long, Long> getAnswerCountMap(Set<Long> qnaIds) {
        Set<Object[]> results = qnaAnswerRepository.countByQnaIdIn(qnaIds);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
