package world.jerry.feedhub.api.application.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.jerry.feedhub.api.domain.member.Member;
import world.jerry.feedhub.api.domain.member.MemberRepository;
import world.jerry.feedhub.api.domain.rss.RssInfo;
import world.jerry.feedhub.api.domain.rss.RssInfoRepository;
import world.jerry.feedhub.api.domain.subscription.Subscription;
import world.jerry.feedhub.api.domain.subscription.SubscriptionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final RssInfoRepository rssInfoRepository;

    @Transactional
    public void subscribe(Long memberId, Long rssInfoId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + memberId));

        RssInfo rssInfo = rssInfoRepository.findById(rssInfoId)
                .orElseThrow(() -> new IllegalArgumentException("RSS not found: " + rssInfoId));

        if (subscriptionRepository.existsByMemberIdAndRssInfoId(memberId, rssInfoId)) {
            return;
        }

        Subscription subscription = new Subscription(member, rssInfo);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(Long memberId, Long rssInfoId) {
        if (memberRepository.findById(memberId).isEmpty()) {
            throw new IllegalArgumentException("User not found: " + memberId);
        }

        subscriptionRepository.findByMemberIdAndRssInfoId(memberId, rssInfoId)
                .ifPresent(subscriptionRepository::delete);
    }

    public List<Long> listSubscriptions(Long memberId) {
        if (memberRepository.findById(memberId).isEmpty()) {
            throw new IllegalArgumentException("User not found: " + memberId);
        }

        List<Subscription> subscriptions = subscriptionRepository.findAllByMemberId(memberId);
        return subscriptions.stream()
                .map(sub -> sub.getRssInfo().getId())
                .collect(Collectors.toList());
    }
}
