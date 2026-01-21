# HISTORY.md

AI Use History - Claude Code 작업 기록

## 2026-01-21

### Bugfix: Tag 추가 시 좋아요 버튼 비활성화 및 좋아요 수 사라지는 현상 해소

**문제**
- Tag 추가 후 피드 카드의 좋아요 수가 0으로 초기화됨
- 좋아요 상태(isLiked)가 false로 초기화됨

**원인**
- `FeedEntryService.updateTags()` 메서드가 태그 업데이트 응답 시 `FeedEntryInfo.from(entry, blogName, siteUrl, tags)` 호출
- 해당 오버로드 메서드가 내부적으로 `likeCount=0L`, `isLiked=false`를 하드코딩하여 반환

**수정 내용**
- 파일: `feed-hub-api/src/main/java/world/jerry/feedhub/api/application/feed/FeedEntryService.java`
- `updateTags()` 메서드에서 실제 좋아요 정보를 조회하여 응답에 포함하도록 수정

```java
// 좋아요 정보 조회
long likeCount = feedLikeRepository.countByFeedEntryId(feedEntryId);
boolean isLiked = feedLikeRepository.existsByMemberIdAndFeedEntryId(memberId, feedEntryId);

return FeedEntryInfo.from(feedEntry, blogName, siteUrl, feedEntry.getTags(), false, likeCount, isLiked);
```
