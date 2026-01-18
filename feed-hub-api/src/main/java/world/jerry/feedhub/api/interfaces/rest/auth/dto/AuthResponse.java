package world.jerry.feedhub.api.interfaces.rest.auth.dto;

import world.jerry.feedhub.api.application.auth.dto.AuthResult;
import world.jerry.feedhub.api.application.auth.dto.MemberInfo;

public record AuthResponse(
        String accessToken,
        UserInfo user
) {
    public record UserInfo(
            Long id,
            String email,
            String nickname
    ) {
        public static UserInfo from(MemberInfo member) {
            return new UserInfo(member.id(), member.email(), member.nickname());
        }
    }

    public static AuthResponse from(AuthResult result) {
        return new AuthResponse(
                result.accessToken(),
                UserInfo.from(result.member())
        );
    }
}
