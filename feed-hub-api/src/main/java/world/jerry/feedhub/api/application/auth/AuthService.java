package world.jerry.feedhub.api.application.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.application.auth.dto.AuthResult;
import world.jerry.feedhub.api.application.auth.dto.MemberInfo;
import world.jerry.feedhub.api.application.auth.dto.SignInCommand;
import world.jerry.feedhub.api.application.auth.dto.SignUpCommand;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;
import world.jerry.feedhub.api.infrastructure.security.JwtProvider;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResult signUp(SignUpCommand command) {
        // Check if email already exists
        if (memberRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다: " + command.email());
        }

        // Encode password and create member
        String encodedPassword = passwordEncoder.encode(command.password());
        Member member = new Member(command.email(), encodedPassword, command.nickname());
        Member savedMember = memberRepository.save(member);

        log.info("새 회원 가입: {}", savedMember.getEmail());

        // Generate token
        String accessToken = jwtProvider.generateToken(savedMember.getId(), savedMember.getEmail());

        return new AuthResult(
                accessToken,
                new MemberInfo(savedMember.getId(), savedMember.getEmail(), savedMember.getNickname())
        );
    }

    @Transactional(readOnly = true)
    public AuthResult signIn(SignInCommand command) {
        Member member = memberRepository.findByEmail(command.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(command.password(), member.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        log.info("로그인 성공: {}", member.getEmail());

        String accessToken = jwtProvider.generateToken(member.getId(), member.getEmail());

        return new AuthResult(
                accessToken,
                new MemberInfo(member.getId(), member.getEmail(), member.getNickname())
        );
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !memberRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public MemberInfo getMemberInfo(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        return new MemberInfo(member.getId(), member.getEmail(), member.getNickname());
    }
}
