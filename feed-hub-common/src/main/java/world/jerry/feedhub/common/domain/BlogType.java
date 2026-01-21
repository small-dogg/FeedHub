package world.jerry.feedhub.common.domain;

/**
 * 블로그 플랫폼 유형
 */
public enum BlogType {
    TISTORY,       // *.tistory.com
    MEDIUM,        // medium.com/@*, *.medium.com
    VELOG,         // velog.io/@*
    GITHUB_BLOG,   // *.github.io
    UNKNOWN        // 알 수 없는 유형 (RSS만 사용)
}
