# FeedHub

RSS 피드를 한눈에 관리하고 조회할 수 있는 웹 애플리케이션입니다.

## Preview

<video src="PREVIEW.mp4" controls width="100%"></video>

> 동영상이 표시되지 않으면 [PREVIEW.mp4](./PREVIEW.mp4) 파일을 직접 다운로드하여 확인하세요.

## 프로젝트 구조

```
FeedHub/
├── feed-hub-api/        # Spring Boot 백엔드 API
├── feed-hub-scheduler/  # 스케줄러 모듈 (RSS 동기화)
└── feed-hub-ui/         # React 프론트엔드
```

## 기술 스택

### Backend (feed-hub-api)
- Java 21
- Spring Boot 4.0.1
- Spring Security + JWT
- Spring Data JPA
- QueryDSL 5.1.0
- PostgreSQL
- Flyway (DB 마이그레이션)
- Lombok

### Frontend (feed-hub-ui)
- React 18 + TypeScript
- Vite
- Axios

## 주요 기능

### 1. 회원 인증
- 이메일/비밀번호 기반 회원가입
- JWT 토큰 기반 로그인
- 로그인 상태 유지

### 2. RSS 소스 관리
- RSS 소스 등록/삭제/조회
- OPML 파일 가져오기/내보내기
- 개별/전체 RSS 동기화

### 3. 피드 조회
- 전체 피드 목록 조회
- RSS 소스별 필터링
- 태그별 필터링 (OR 조건)
- 제목/내용 텍스트 검색
- 커서 기반 페이지네이션 (무한 스크롤)

### 4. 컨텐츠 미리보기
- 피드 카드에서 바로 미리보기
- 플로팅 모달로 description 표시
- ESC 키 또는 X 버튼으로 닫기
- 모달 내 원문보기 버튼

### 5. 사용자별 태그 관리
- 로그인 사용자별 개인 태그
- 피드에 태그 추가/삭제
- 태그 클릭으로 필터링

### 6. 읽음/읽지않음 관리
- 피드별 읽음 상태 표시 (로그인 시)
- 제목/미리보기/원문보기 클릭 시 읽음 처리
- 조회수 카운팅 (중복 방지)

### 7. 관리자 기능
- 플로팅 버튼을 통한 관리자 모달
- RSS 소스 추가/삭제
- 태그 추가/삭제
- OPML 가져오기/내보내기

## API 엔드포인트

### 인증
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/signup` | 회원가입 |
| POST | `/api/v1/auth/signin` | 로그인 |
| GET | `/api/v1/auth/check-email` | 이메일 중복 확인 |
| GET | `/api/v1/auth/me` | 현재 사용자 정보 |

### RSS 소스 관리
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/rss-sources` | RSS 소스 등록 |
| GET | `/api/v1/rss-sources` | 전체 RSS 소스 조회 |
| DELETE | `/api/v1/rss-sources/{id}` | RSS 소스 삭제 |
| POST | `/api/v1/rss-sources/{id}/sync` | 개별 동기화 |
| POST | `/api/v1/rss-sources/sync-all` | 전체 동기화 |
| POST | `/api/v1/rss-sources/import/opml` | OPML 가져오기 |
| GET | `/api/v1/rss-sources/export/opml` | OPML 내보내기 |

### 태그 관리
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/tags` | 태그 생성 |
| GET | `/api/v1/tags` | 전체 태그 조회 (사용자별) |
| DELETE | `/api/v1/tags/{id}` | 태그 삭제 |

### 피드 조회
| Method | Path | Query Params | 설명 |
|--------|------|--------------|------|
| GET | `/api/v1/feeds` | `rssSourceIds`, `tagIds`, `query`, `lastId`, `lastPublishedAt`, `size` | 피드 검색 |
| PUT | `/api/v1/feeds/{id}/tags` | - | 피드 태그 업데이트 |
| POST | `/api/v1/feeds/{id}/view` | - | 읽음 처리 및 조회수 증가 |

## 데이터베이스 스키마

```sql
-- 회원
member (id, email, password, nickname, created_at, updated_at)

-- RSS 소스 정보
rss_info (id, blog_name, author, rss_url, site_url, language, created_at, last_sync_at)

-- 태그 (사용자별)
tag (id, member_id, name, created_at)

-- RSS 소스-태그 연결 (다대다)
rss_info_tag (rss_info_id, tag_id)

-- 피드 엔트리
feed_entry (id, rss_info_id, title, link, description, author, published_at, guid, created_at, view_count)

-- 피드 엔트리-태그 연결 (다대다)
feed_entry_tag (feed_entry_id, tag_id)

-- 회원 피드 읽음 기록
member_feed_read (id, member_id, feed_entry_id, read_at)
```

## 실행 방법

### 사전 요구사항
- Java 21+
- Node.js 18+
- PostgreSQL

### Backend 실행

```bash
# 프로젝트 빌드
./gradlew :feed-hub-api:build

# 애플리케이션 실행
./gradlew :feed-hub-api:bootRun
```

> **Note**: `application.yml`에서 데이터베이스 연결 정보와 JWT 시크릿 키를 설정하세요.

### Frontend 실행

```bash
cd feed-hub-ui

# 의존성 설치
npm install

# 개발 서버 실행 (port 3000)
npm run dev

# 프로덕션 빌드
npm run build
```

## 프론트엔드 구조

```
feed-hub-ui/src/
├── api/
│   └── client.ts              # API 클라이언트 (axios, 토큰 관리)
├── types/
│   └── index.ts               # TypeScript 타입 정의
├── components/
│   ├── FeedCard.tsx           # 피드 카드 컴포넌트
│   ├── FeedList.tsx           # 피드 목록 컴포넌트
│   ├── FilterBar.tsx          # 필터 바 (RSS 소스, 태그, 검색)
│   ├── ContentPreviewModal.tsx # 컨텐츠 미리보기 모달
│   ├── TagSelectModal.tsx     # 태그 선택 모달
│   ├── AuthModal.tsx          # 로그인/회원가입 모달
│   ├── AdminModal.tsx         # 관리자 모달
│   └── AdminButton.tsx        # 플로팅 관리 버튼
├── App.tsx                    # 메인 앱
└── App.css                    # 앱 스타일
```

## 백엔드 구조 (DDD)

```
feed-hub-api/src/main/java/world/jerry/feedhub/api/
├── domain/                    # 도메인 레이어
│   ├── rss/                  # RssInfo 엔티티, 리포지토리
│   ├── tag/                  # Tag 엔티티, 리포지토리
│   ├── feed/                 # FeedEntry, MemberFeedRead 엔티티, 리포지토리
│   └── member/               # Member 엔티티, 리포지토리
├── application/              # 애플리케이션 레이어
│   ├── rss/                  # RssInfoService, DTO
│   ├── tag/                  # TagService, DTO
│   ├── feed/                 # FeedQueryService, FeedEntryService, DTO
│   └── auth/                 # AuthService, DTO
├── infrastructure/           # 인프라 레이어
│   ├── persistence/          # JPA 구현체, QueryDSL
│   ├── security/             # JWT, Spring Security 설정
│   └── config/               # QueryDSL 설정
└── interfaces/               # 인터페이스 레이어
    ├── rest/                 # REST 컨트롤러
    └── common/               # 공통 (GlobalExceptionHandler)
```

## 라이선스

MIT License
