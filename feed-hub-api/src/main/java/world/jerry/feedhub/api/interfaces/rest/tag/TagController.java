package world.jerry.feedhub.api.interfaces.rest.tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody CreateTagRequest request) {
        TagInfo info = tagService.createTag(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(info));
    }

    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<TagResponse> tags = tagService.getAllTags().stream()
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
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }
}
