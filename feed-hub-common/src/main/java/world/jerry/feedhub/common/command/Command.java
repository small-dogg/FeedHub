package world.jerry.feedhub.common.command;

import java.time.Instant;

/**
 * 모든 Command의 기본 인터페이스
 * Command는 시스템에 요청하는 작업을 나타냄
 */
public interface Command {

    /**
     * Command의 고유 식별자 (멱등성 보장용)
     */
    String commandId();

    /**
     * Command 요청 시각
     */
    Instant requestedAt();
}
