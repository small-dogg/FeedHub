# FeedHub

RSS 피드를 한눈에 관리하고 조회할 수 있는 웹 애플리케이션입니다.

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

### 1. RSS 소스 관리
- RSS 소스 등록/삭제/조회
- 태그를 통한 RSS 소스 분류

### 2. 피드 조회
- 전체 피드 목록 조회
- RSS 소스별 필터링
- 태그별 필터링 (OR 조건)
- 최신순 정렬
- 페이지네이션

### 3. 관리자 기능
- 플로팅 버튼을 통한 관리자 모달
- RSS 소스 추가/삭제
- 태그 추가/삭제

## API 엔드포인트

### RSS 소스 관리
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/rss-sources` | RSS 소스 등록 |
| GET | `/api/v1/rss-sources` | 전체 RSS 소스 조회 |
| GET | `/api/v1/rss-sources/{id}` | RSS 소스 상세 조회 |
| PUT | `/api/v1/rss-sources/{id}/tags` | 태그 업데이트 |
| DELETE | `/api/v1/rss-sources/{id}` | RSS 소스 삭제 |

### 태그 관리
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/tags` | 태그 생성 |
| GET | `/api/v1/tags` | 전체 태그 조회 |
| DELETE | `/api/v1/tags/{id}` | 태그 삭제 |

### 피드 조회
| Method | Path | Query Params | 설명 |
|--------|------|--------------|------|
| GET | `/api/v1/feeds` | `rssSourceIds`, `tagIds`, `page`, `size` | 피드 검색 |

## 데이터베이스 스키마

```sql
-- RSS 소스 정보
rss_info (id, blog_name, author, rss_url, site_url, language, created_at, last_sync_at)

-- 태그
tag (id, name, created_at)

-- RSS 소스-태그 연결 (다대다)
rss_info_tag (rss_info_id, tag_id)

-- 피드 엔트리
feed_entry (id, rss_info_id, title, link, description, author, published_at, guid, created_at)
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

> **Note**: `application.yml`에서 데이터베이스 연결 정보를 설정하세요.

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
│   └── client.ts           # API 클라이언트
├── types/
│   └── index.ts            # TypeScript 타입 정의
├── components/
│   ├── FeedCard.tsx        # 피드 카드 컴포넌트
│   ├── FeedList.tsx        # 피드 목록 컴포넌트
│   ├── FilterBar.tsx       # 필터 바 (RSS 소스, 태그 선택)
│   ├── Pagination.tsx      # 페이지네이션
│   ├── AdminModal.tsx      # 관리자 모달
│   └── AdminButton.tsx     # 플로팅 관리 버튼
├── App.tsx                 # 메인 앱
└── index.css               # 글로벌 스타일
```

## 백엔드 구조 (DDD)

```
feed-hub-api/src/main/java/world/jerry/feedhub/api/
├── domain/                 # 도메인 레이어
│   ├── rss/               # RssInfo 엔티티, 리포지토리
│   ├── tag/               # Tag 엔티티, 리포지토리
│   └── feed/              # FeedEntry 엔티티, 리포지토리
├── application/           # 애플리케이션 레이어
│   ├── rss/               # RssInfoService, DTO
│   ├── tag/               # TagService, DTO
│   └── feed/              # FeedQueryService, DTO
├── infrastructure/        # 인프라 레이어
│   ├── persistence/       # JPA 구현체, QueryDSL
│   └── config/            # QueryDSL 설정
└── interfaces/            # 인터페이스 레이어
    ├── rest/              # REST 컨트롤러
    └── common/            # 공통 (GlobalExceptionHandler)
```

## 라이선스

MIT License
