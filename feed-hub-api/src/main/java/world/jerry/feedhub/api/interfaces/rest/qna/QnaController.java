package world.jerry.feedhub.api.interfaces.rest.qna;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.qna.QnaService;
import world.jerry.feedhub.api.application.qna.dto.QnaAnswerInfo;
import world.jerry.feedhub.api.application.qna.dto.QnaDetailInfo;
import world.jerry.feedhub.api.application.qna.dto.QnaInfo;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;
import world.jerry.feedhub.api.interfaces.rest.qna.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;
    private final MemberRepository memberRepository;

    /**
     * QnA 질문 등록
     */
    @PostMapping
    public ResponseEntity<QnaResponse> createQuestion(
            @AuthenticationPrincipal Long memberId,
            @RequestBody CreateQnaRequest request
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        QnaInfo qnaInfo = qnaService.createQuestion(request.toCommand(memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(QnaResponse.from(qnaInfo));
    }

    /**
     * 내 QnA 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<QnaResponse>> getMyQuestions(
            @AuthenticationPrincipal Long memberId
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<QnaInfo> qnaList = qnaService.getMyQuestions(memberId);
        List<QnaResponse> responses = qnaList.stream()
                .map(QnaResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * 전체 QnA 목록 조회 (관리자 전용)
     */
    @GetMapping("/all")
    public ResponseEntity<List<QnaResponse>> getAllQuestions(
            @AuthenticationPrincipal Long memberId
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 관리자 권한 체크
        if (!isAdmin(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<QnaInfo> qnaList = qnaService.getAllQuestions();
        List<QnaResponse> responses = qnaList.stream()
                .map(QnaResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * QnA 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<QnaDetailResponse> getQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean isAdmin = isAdmin(memberId);
        QnaDetailInfo qnaDetail = qnaService.getQuestion(id, memberId, isAdmin);
        return ResponseEntity.ok(QnaDetailResponse.from(qnaDetail));
    }

    /**
     * QnA 삭제 (본인만)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        qnaService.deleteQuestion(id, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * QnA 답변 등록 (관리자 전용)
     */
    @PostMapping("/{id}/answer")
    public ResponseEntity<QnaDetailResponse.AnswerResponse> createAnswer(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id,
            @RequestBody CreateAnswerRequest request
    ) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 관리자 권한 체크
        if (!isAdmin(memberId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        QnaAnswerInfo answerInfo = qnaService.createAnswer(id, request.toCommand(memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(QnaDetailResponse.AnswerResponse.from(answerInfo));
    }

    private boolean isAdmin(Long memberId) {
        return memberRepository.findById(memberId)
                .map(Member::isAdmin)
                .orElse(false);
    }
}
