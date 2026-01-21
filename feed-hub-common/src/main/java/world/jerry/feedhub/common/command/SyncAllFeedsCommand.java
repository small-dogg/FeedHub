package world.jerry.feedhub.common.command;

import java.time.Instant;

/**
 * 전체 RSS 피드 동기화 요청 Command (Scheduler에서 사용)
 */
public record SyncAllFeedsCommand(
        String commandId,
        Instant requestedAt,
        String requestedBy
) implements Command {
}
