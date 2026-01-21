package world.jerry.feedhub.api.interfaces.rest.rss.dto;

/**
 * Response for async command requests
 */
public record AsyncCommandResponse(
        String commandId,
        String status,
        String message
) {
    public static AsyncCommandResponse accepted(String commandId, String message) {
        return new AsyncCommandResponse(commandId, "ACCEPTED", message);
    }

    public static AsyncCommandResponse syncAccepted(String commandId) {
        return accepted(commandId, "동기화 요청이 접수되었습니다");
    }

    public static AsyncCommandResponse syncAllAccepted(String commandId) {
        return accepted(commandId, "전체 동기화 요청이 접수되었습니다");
    }
}
