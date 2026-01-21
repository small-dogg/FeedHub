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

---

### Feature: 좋아요 목록 보기 및 정렬 기능 추가

**요구사항**
- 내가 좋아요한 피드 목록 보기
- 좋아요 수 순 정렬
- 게시날짜 순 정렬 (기존)
- 정렬 옵션 선택 기능

**구현 내용**

1. **정렬 타입 (FeedSortType)**
   - `PUBLISHED_AT`: 게시날짜 순 (기본값)
   - `LIKE_COUNT`: 좋아요 수 순
   - `LIKED_AT`: 좋아요한 시간 순 (likedOnly=true일 때)

2. **백엔드 수정 파일**
   - `FeedSortType.java` - 신규 생성
   - `FeedSearchCriteria.java` - sortType, likedOnly, lastLikeCount, lastLikedAt 추가
   - `FeedEntryInfo.java` - likedAt 필드 추가
   - `FeedEntryPage.java` - 커서 필드 확장
   - `FeedLikeRepository.java` - likedAt 조회 메서드 추가
   - `FeedEntryQueryRepository.java` - sortType별 쿼리 분기 구현
   - `FeedController.java` - API 파라미터 추가
   - `FeedPageResponse.java` - 커서 필드 확장
   - `FeedEntryResponse.java` - likedAt 필드 추가

3. **프론트엔드 수정 파일**
   - `types/index.ts` - FeedSortType, 커서 필드 추가
   - `api/client.ts` - 새 파라미터 지원
   - `FilterBar.tsx` - 정렬 드롭다운, 좋아요 필터 체크박스 추가
   - `FilterBar.css` - 정렬 UI 스타일
   - `HomePage.tsx` - 정렬 상태 관리 및 API 연동

**API 사용 예시**
```
GET /api/v1/feeds?sortType=PUBLISHED_AT          # 최신순
GET /api/v1/feeds?sortType=LIKE_COUNT            # 좋아요순
GET /api/v1/feeds?likedOnly=true&sortType=LIKED_AT  # 내가 좋아요한 목록
```
