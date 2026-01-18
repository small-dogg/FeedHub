package world.jerry.feedhub.api.interfaces.rest.tag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import world.jerry.feedhub.api.application.tag.dto.CreateTagCommand;

public record CreateTagRequest(
        @NotBlank(message = "Tag name is required")
        @Size(max = 100, message = "Tag name must be at most 100 characters")
        String name
) {
    public CreateTagCommand toCommand(Long memberId) {
        return new CreateTagCommand(memberId, name);
    }
}
