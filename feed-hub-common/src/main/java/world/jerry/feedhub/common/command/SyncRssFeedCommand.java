package world.jerry.feedhub.common.command;

import java.time.Instant;

/**
 * RSS 피드 동기화 요청 Command
 */
public record SyncRssFeedCommand(
        String commandId,
        Long rssInfoId,
        String rssUrl,
        String blogName,
        Instant requestedAt,
        String requestedBy
) implements Command {
}
