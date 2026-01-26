package world.jerry.feedhub.api.interfaces.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.subscription.SubscriptionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

        private final SubscriptionService subscriptionService;

        @PostMapping("/{rssInfoId}")
        public ResponseEntity<Void> subscribe(
                        @AuthenticationPrincipal Long memberId,
                        @PathVariable Long rssInfoId) {
                if (memberId == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                subscriptionService.subscribe(memberId, rssInfoId);
                return ResponseEntity.ok().build();
        }

        @DeleteMapping("/{rssInfoId}")
        public ResponseEntity<Void> unsubscribe(
                        @AuthenticationPrincipal Long memberId,
                        @PathVariable Long rssInfoId) {
                if (memberId == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                subscriptionService.unsubscribe(memberId, rssInfoId);
                return ResponseEntity.ok().build();
        }

        @GetMapping
        public ResponseEntity<List<Long>> listSubscriptions(
                        @AuthenticationPrincipal Long memberId) {
                if (memberId == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                return ResponseEntity.ok(subscriptionService.listSubscriptions(memberId));
        }
}
