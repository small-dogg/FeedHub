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

export interface FeedPage {
  content: FeedEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface FeedSearchParams {
  rssSourceIds?: number[];
  tagIds?: number[];
  page?: number;
  size?: number;
}
