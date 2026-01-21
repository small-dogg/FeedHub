package world.jerry.feedhub.common.event;

import java.time.Instant;

/**
 * 모든 Event의 기본 인터페이스
 * Event는 시스템에서 발생한 사실을 나타냄
 */
public interface Event {

    /**
     * Event의 고유 식별자
     */
    String eventId();

    /**
     * 원본 Command의 ID (연관 추적용)
     */
    String correlationId();

    /**
     * Event 발생 시각
     */
    Instant occurredAt();
}
