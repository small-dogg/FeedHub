package world.jerry.feedhub.api.interfaces.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.domain.subscription.Subscription;
import world.jerry.feedhub.api.domain.subscription.SubscriptionRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final RssInfoRepository rssInfoRepository;

    @PostMapping("/{rssInfoId}")
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long rssInfoId) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        RssInfo rssInfo = rssInfoRepository.findById(rssInfoId)
                .orElseThrow(() -> new IllegalArgumentException("RSS not found: " + rssInfoId));

        if (subscriptionRepository.existsByMemberIdAndRssInfoId(member.getId(), rssInfoId)) {
            return ResponseEntity.ok().build();
        }

        Subscription subscription = new Subscription(member, rssInfo);
        subscriptionRepository.save(subscription);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{rssInfoId}")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long rssInfoId) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        subscriptionRepository.findByMemberIdAndRssInfoId(member.getId(), rssInfoId)
                .ifPresent(subscriptionRepository::delete);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Long>> listSubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        List<Subscription> subscriptions = subscriptionRepository.findAllByMemberId(member.getId());
        List<Long> rssInfoIds = subscriptions.stream()
                .map(sub -> sub.getRssInfo().getId())
                .collect(Collectors.toList());

        return ResponseEntity.ok(rssInfoIds);
    }
}
