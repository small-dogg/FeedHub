package world.jerry.feedhub.api.application.auth.dto;

import world.jerry.feedhub.api.domain.member.Role;

public record MemberInfo(
        Long id,
        String email,
        String nickname,
        Role role
) {
}
