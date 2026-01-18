export interface Tag {
  id: number;
  name: string;
  createdAt: string;
}

export interface RssSource {
  id: number;
  blogName: string;
  author: string | null;
  rssUrl: string;
  siteUrl: string | null;
  language: string | null;
  createdAt: string;
  lastSyncAt: string | null;
  tags: Tag[];
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
}

export interface FeedSlice {
  content: FeedEntry[];
  lastId: number | null;
  lastPublishedAt: string | null;
  hasMore: boolean;
}

export interface FeedSearchParams {
  rssSourceIds?: number[];
  tagIds?: number[];
  query?: string;
  lastId?: number;
  lastPublishedAt?: string;
  size?: number;
}

export interface SyncResult {
  rssSourceId: number;
  blogName: string;
  syncedCount: number;
  skippedCount: number;
  lastSyncAt: string;
}

export interface OpmlImportResult {
  totalFound: number;
  imported: number;
  skipped: number;
  skippedUrls: string[];
  syncResults: SyncResult[];
}

// Auth types
export interface User {
  id: number;
  email: string;
  nickname: string;
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
