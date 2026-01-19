package world.jerry.feedhub.api.interfaces.rest.auth.dto;

import world.jerry.feedhub.api.application.auth.dto.AuthResult;
import world.jerry.feedhub.api.application.auth.dto.MemberInfo;
import world.jerry.feedhub.api.domain.member.Role;

public record AuthResponse(
        String accessToken,
        UserInfo user
) {
    public record UserInfo(
            Long id,
            String email,
            String nickname,
            Role role
    ) {
        public static UserInfo from(MemberInfo member) {
            return new UserInfo(member.id(), member.email(), member.nickname(), member.role());
        }
    }

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                UserInfo.from(result.member())
        );
    }
}
