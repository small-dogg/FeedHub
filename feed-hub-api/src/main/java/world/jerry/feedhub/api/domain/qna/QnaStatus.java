package world.jerry.feedhub.api.domain.qna;

/**
 * QnA 상태
 */
public enum QnaStatus {
    PENDING,   // 답변 대기
    ANSWERED,  // 답변 완료
    CLOSED     // 종료
}
