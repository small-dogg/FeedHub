package world.jerry.feedhub.api.interfaces.rest.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import world.jerry.feedhub.api.application.tag.TagService;
import world.jerry.feedhub.api.application.tag.dto.TagInfo;
import world.jerry.feedhub.api.interfaces.rest.tag.dto.CreateTagRequest;
import world.jerry.feedhub.api.interfaces.rest.tag.dto.TagResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateTagRequest request) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TagInfo info = tagService.createTag(request.toCommand(memberId));
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(info));
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags(@AuthenticationPrincipal Long memberId) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<TagResponse> tags = tagService.getAllTagsByMember(memberId).stream()
                .map(TagResponse::from)
                .toList();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagResponse> getTag(@PathVariable Long id) {
        TagInfo info = tagService.getTag(id);
        return ResponseEntity.ok(TagResponse.from(info));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long id) {
        if (memberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        tagService.deleteTag(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
