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
  };
  title: string;
  link: string;
  description: string | null;
  author: string | null;
  publishedAt: string | null;
}

export interface FeedSlice {
  content: FeedEntry[];
  lastId: number | null;
  hasMore: boolean;
}

export interface FeedSearchParams {
  rssSourceIds?: number[];
  tagIds?: number[];
  lastId?: number;
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
