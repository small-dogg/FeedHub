export type BlogType = 'TISTORY' | 'MEDIUM' | 'VELOG' | 'GITHUB_BLOG' | 'UNKNOWN';

export interface Tag {
  id: number;
  name: string;
  createdAt: string;
}

export interface TagSuggestion {
  name: string;
  userCount: number;
}

export interface RssSource {
  id: number;
  blogName: string;
  author: string | null;
  rssUrl: string;
  siteUrl: string | null;
  crawlUrl: string | null;
  language: string | null;
  blogType: BlogType | null;
  createdAt: string;
  lastSyncAt: string | null;
  tags?: Tag[];
}

export interface FeedEntry {
  id: number;
  rssSource: {
    id: number;
    blogName: string;
    siteUrl: string | null;
  };
  title: string;
  link: string;
  description: string | null;
  author: string | null;
  publishedAt: string | null;
  viewCount: number;
  tags: { id: number; name: string }[];
  isRead: boolean;
  likeCount: number;
  isLiked: boolean;
  likedAt: string | null;
}

export interface LikeResponse {
  feedEntryId: number;
  liked: boolean;
  likeCount: number;
}

export type FeedSortType = 'PUBLISHED_AT' | 'LIKE_COUNT' | 'LIKED_AT';

export interface FeedSlice {
  content: FeedEntry[];
  lastId: number | null;
  lastPublishedAt: string | null;
  lastLikeCount: number | null;
  lastLikedAt: string | null;
  hasMore: boolean;
}

export interface FeedSearchParams {
  rssSourceIds?: number[];
  tagIds?: number[];
  query?: string;
  lastId?: number;
  lastPublishedAt?: string;
  lastLikeCount?: number;
  lastLikedAt?: string;
  sortType?: FeedSortType;
  likedOnly?: boolean;
  size?: number;
}

export interface SyncResult {
  commandId: string;
  status: string;
  message: string;
}

export interface CrawlResult {
  rssSourceId: number;
  blogName: string;
  blogType: string;
  requested: boolean;
  message: string | null;
}

export interface OpmlImportResult {
  totalFound: number;
  imported: number;
  skipped: number;
  skippedUrls: string[];
  syncRequestedIds: number[];
  message: string;
}

// Auth types
export type Role = 'USER' | 'ADMIN';

export interface User {
  id: number;
  email: string;
  nickname: string;
  role: Role;
}

export interface AuthResponse {
  accessToken: string;
  user: User;
}

export interface SignUpRequest {
  email: string;
  password: string;
  passwordConfirm: string;
  nickname: string;
}

export interface SignInRequest {
  email: string;
  password: string;
}

// QnA types
export type QnaType = 'GENERAL' | 'FEATURE_REQUEST' | 'BUG_REPORT' | 'RSS_REQUEST';
export type QnaStatus = 'PENDING' | 'ANSWERED' | 'CLOSED';

export interface Qna {
  id: number;
  memberNickname: string;
  type: QnaType;
  title: string;
  isSecret: boolean;
  status: QnaStatus;
  createdAt: string;
  answerCount: number;
}

export interface QnaAnswer {
  id: number;
  memberNickname: string;
  content: string;
  createdAt: string;
}

export interface QnaDetail extends Qna {
  content: string;
  updatedAt: string | null;
  answers: QnaAnswer[];
}

export interface CreateQnaRequest {
  type: QnaType;
  title: string;
  content: string;
  isSecret: boolean;
}

export interface CreateAnswerRequest {
  content: string;
}
