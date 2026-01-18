package world.jerry.feedhub.api.interfaces.rest.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.auth.AuthService;
import world.jerry.feedhub.api.application.auth.dto.AuthResult;
import world.jerry.feedhub.api.application.auth.dto.MemberInfo;
import world.jerry.feedhub.api.application.auth.dto.SignInCommand;
import world.jerry.feedhub.api.application.auth.dto.SignUpCommand;
import world.jerry.feedhub.api.interfaces.rest.auth.dto.AuthResponse;
import world.jerry.feedhub.api.interfaces.rest.auth.dto.EmailCheckResponse;
import world.jerry.feedhub.api.interfaces.rest.auth.dto.SignInRequest;
import world.jerry.feedhub.api.interfaces.rest.auth.dto.SignUpRequest;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        // Validate password confirmation
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        SignUpCommand command = new SignUpCommand(
                request.email(),
                request.password(),
                request.nickname()
        );

        AuthResult result = authService.signUp(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.from(result));
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody SignInRequest request) {
        SignInCommand command = new SignInCommand(request.email(), request.password());
        AuthResult result = authService.signIn(command);
        return ResponseEntity.ok(AuthResponse.from(result));
    }

    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(new EmailCheckResponse(available));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserInfo> getCurrentMember(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MemberInfo member = authService.getMemberInfo(memberId);
        return ResponseEntity.ok(AuthResponse.UserInfo.from(member));
    }
}
