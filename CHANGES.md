# Changelog

프로젝트의 주요 변경사항을 주 단위로 기록합니다.

## Week of 2026-01-27 (Jan 27)

### 운영 배포 준비
- **feat**: AWS Parameter Store 연동 설정
  - API, Collector 모듈에 Spring Cloud AWS 의존성 추가
  - 운영 환경용 application-prod.yml 생성 (api, collector, scheduler)
  - DB credentials 및 JWT secret을 Parameter Store에서 로드하도록 구성

## Week of 2026-01-20 (Jan 20-26)

### 구독 및 개인화 기능
- **feat**: 사용자별 RSS 소스 구독 기능 구현
  - 구독 엔티티 및 리포지토리 추가
  - 구독한 소스의 피드만 조회 가능하도록 필터링
- **refactor**: 구독 아키텍처 및 보안 개선

### 크롤링 시스템 개선
- **refactor**: EDA 기반 MSA 구조로 리팩토링
  - Kafka Command/Event 패턴 도입
  - feed-hub-common 모듈 추가 (공유 도메인 및 메시지)
  - Fan-out 구조로 RSS 동기화 개선
- **feat**: 크롤링 이력 관리 (sync_history 테이블)
  - 동기화 상태 추적 및 에러 로깅
  - sync_type CHECK 제약조건 추가
- **feat**: Collector에 타입 안전 KafkaTemplate 설정
- **fix**: 스케줄러 Kafka 서버 포트 수정

### 피드 기능 개선
- **feat**: 좋아요 목록 보기 및 정렬 기능
- **feat**: 피드별 태그 추천 기능
- **fix**: 태그 추가 시 좋아요 정보 유지되도록 수정
- **feat**: 사용자별 피드 좋아요 기능 추가

### 기타
- **feat**: QnA 서비스 기능 추가
  - 문의 유형별 질문/답변 관리
  - 비밀글 지원
- **feat**: RssInfo에 crawlUrl 필드 추가 및 Medium 크롤러 개선
- **docs**: README.md 업데이트 및 미리보기 링크 수정
- **chore**: 디렉팅 문서 작성 및 서비스 방향 정의
- **chore**: CLAUDE.md 프로젝트 규칙 수정

## Week of 2026-01-13 (Jan 13-19)

### 크롤러 모듈 추가
- **feat**: Kafka 기반 블로그 크롤러 모듈 추가
  - feed-hub-crawler (현재 feed-hub-collector로 변경됨)
  - 블로그 타입별 전용 크롤러 (Tistory, Velog, Medium, GitHub Blog)
  - Rate limiting 및 Circuit Breaker 적용

### 크롤러 안정성 개선
- **fix**: VelogCrawler cursor 업데이트 버그 수정
- **fix**: URL 정규화로 중복 아티클 감지 개선
- **fix**: 모든 크롤러에 중복 아티클 감지 로직 적용
- **fix**: 크롤링 시 중복 아티클로 마지막 페이지 판단
- **refactor**: 크롤러 로깅 개선 및 Thread.sleep 제거
- **fix**: crawler application.yml 오타 수정

### 인증 및 권한
- **feat**: JWT 기반 로그인/회원가입 기능
  - Spring Security 설정
  - 토큰 발급 및 검증
  - 역할 기반 권한 제어 (USER/ADMIN)
- **feat**: RSS 소스 관리 권한 제어 개선

### 피드 조회 및 상호작용
- **feat**: 컨텐츠 미리보기 기능
  - 플로팅 모달로 description 표시
  - ESC 키 또는 X 버튼으로 닫기
- **feat**: 피드 읽음/읽지않음 상태 표시 및 조회수 카운팅
  - 중복 카운팅 방지
  - 읽음 상태 시각적 표시
- **feat**: 피드 조회수(viewCount) 기능 추가
- **feat**: 피드 카드에서 RSS 블로그 이름 클릭 시 사이트로 이동
- **feat**: 피드 제목/내용 텍스트 검색 기능

### 태그 시스템 개선
- **feat**: 태그를 사용자별로 관리하도록 변경
  - 개인별 독립적인 태그 관리
- **refactor**: 태그 기능을 RSS 소스에서 피드 엔트리 레벨로 마이그레이션
  - 더 세밀한 태그 관리 가능
- **fix**: 태그 추가/제거 후 해당 피드만 즉시 반영

### RSS 및 OPML
- **feat**: OPML 내보내기 기능
- **feat**: RSS 동기화 스케줄링 및 최초 등록 시 자동 동기화
- **fix**: Atom 피드의 updated 필드에서 발행일자 추출 지원

### 문서 및 미디어
- **docs**: README.md 최신 버전으로 업데이트
- **docs**: 미리보기 섹션 수정
- **chore**: Git LFS로 동영상 파일 관리

## Week of 2026-01-06 (Jan 11-12)

### 프로젝트 초기화
- **chore**: Gradle 멀티 모듈 프로젝트 구조 초기화
  - feed-hub-api: 백엔드 API 모듈
  - feed-hub-ui: React 프론트엔드 모듈

### 기본 기능 구현
- **feat**: RSS 관리 기능 및 UI 개발
  - RSS 소스 등록/수정/삭제
  - RSS 피드 목록 조회
- **feat**: Feed 동기화 작업 완료
  - OPML 파일 임포트 기능 개발
- **feat**: Feed 목록 publishedAt 정렬
- **feat**: 태그 기능 기본 구현

---

## 아키텍처 변화

### EDA 기반 MSA (2026-01-21)
- 모놀리식에서 이벤트 드리븐 마이크로서비스로 전환
- Kafka를 통한 모듈 간 느슨한 결합
- Command/Event 패턴으로 비동기 처리
- 독립적인 스케일링 및 배포 가능

### 모듈 구조
```
feed-hub-common     # 공유 도메인 및 Kafka 메시지
feed-hub-api        # REST API 서버
feed-hub-collector  # RSS 수집 및 크롤링
feed-hub-scheduler  # 정기 동기화 스케줄러
feed-hub-ui         # React 프론트엔드
```

### 인프라
- PostgreSQL: 영속성 레이어
- Apache Kafka: 이벤트 스트리밍
- AWS Parameter Store: 보안 설정 관리 (운영 환경)