package world.jerry.feedhub.api.application.auth.dto;

public record SignInCommand(
        String email,
        String password
) {
}
