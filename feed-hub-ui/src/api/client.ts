import axios from 'axios';
import type {
  FeedEntry,
  FeedSlice,
  FeedSearchParams,
  LikeResponse,
  OpmlImportResult,
  RssSource,
  SyncResult,
  CrawlResult,
  Tag,
  TagSuggestion,
  AuthResponse,
  SignUpRequest,
  SignInRequest,
  User,
  Qna,
  QnaDetail,
  QnaAnswer,
  CreateQnaRequest,
  CreateAnswerRequest,
} from '../types';

const API_BASE = '/api/v1';
const TOKEN_KEY = 'feedhub_access_token';
const SAVED_EMAIL_KEY = 'feedhub_saved_email';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Token management
export const tokenManager = {
  // Check localStorage first, then sessionStorage
  getToken: (): string | null =>
    localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY),
  // persistent=true → localStorage (auto-login), false → sessionStorage (session only)
  setToken: (token: string, persistent: boolean): void => {
    if (persistent) {
      localStorage.setItem(TOKEN_KEY, token);
      sessionStorage.removeItem(TOKEN_KEY);
    } else {
      sessionStorage.setItem(TOKEN_KEY, token);
      localStorage.removeItem(TOKEN_KEY);
    }
  },
  removeToken: (): void => {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
  },
};

// Email save/restore management
export const emailManager = {
  getSavedEmail: (): string | null => localStorage.getItem(SAVED_EMAIL_KEY),
  saveEmail: (email: string): void => localStorage.setItem(SAVED_EMAIL_KEY, email),
  removeEmail: (): void => localStorage.removeItem(SAVED_EMAIL_KEY),
};

// Add auth header interceptor
api.interceptors.request.use((config) => {
  const token = tokenManager.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Feed API
export const feedApi = {
  search: async (params: FeedSearchParams): Promise<FeedSlice> => {
    const searchParams = new URLSearchParams();
    if (params.rssSourceIds?.length) {
      params.rssSourceIds.forEach(id => searchParams.append('rssSourceIds', id.toString()));
    }
    if (params.tagIds?.length) {
      params.tagIds.forEach(id => searchParams.append('tagIds', id.toString()));
    }
    if (params.query) {
      searchParams.append('query', params.query);
    }
    if (params.lastId !== undefined) {
      searchParams.append('lastId', params.lastId.toString());
    }
    if (params.lastPublishedAt !== undefined) {
      searchParams.append('lastPublishedAt', params.lastPublishedAt);
    }
    if (params.lastLikeCount !== undefined) {
      searchParams.append('lastLikeCount', params.lastLikeCount.toString());
    }
    if (params.lastLikedAt !== undefined) {
      searchParams.append('lastLikedAt', params.lastLikedAt);
    }
    if (params.sortType) {
      searchParams.append('sortType', params.sortType);
    }
    if (params.likedOnly !== undefined) {
      searchParams.append('likedOnly', params.likedOnly.toString());
    }
    if (params.mode) {
      searchParams.append('mode', params.mode);
    }
    if (params.size !== undefined) {
      searchParams.append('size', params.size.toString());
    }
    const response = await api.get<FeedSlice>(`/feeds?${searchParams.toString()}`);
    return response.data;
  },

  updateTags: async (feedId: number, tagIds: number[], newTagNames: string[]): Promise<FeedEntry> => {
    const response = await api.put<FeedEntry>(`/feeds/${feedId}/tags`, { tagIds, newTagNames });
    return response.data;
  },

  incrementViewCount: async (feedId: number): Promise<void> => {
    await api.post(`/feeds/${feedId}/view`);
  },

  toggleLike: async (feedId: number): Promise<LikeResponse> => {
    const response = await api.post<LikeResponse>(`/feeds/${feedId}/like`);
    return response.data;
  },
};

// Subscription API
export const subscriptionApi = {
  subscribe: async (rssInfoId: number): Promise<void> => {
    await api.post(`/subscriptions/${rssInfoId}`);
  },

  unsubscribe: async (rssInfoId: number): Promise<void> => {
    await api.delete(`/subscriptions/${rssInfoId}`);
  },

  getMySubscriptions: async (): Promise<number[]> => {
    const response = await api.get<number[]>('/subscriptions');
    return response.data;
  },
};

// RSS Source API
export const rssSourceApi = {
  getAll: async (): Promise<RssSource[]> => {
    const response = await api.get<RssSource[]>('/rss-sources');
    return response.data;
  },

  create: async (rssUrl: string): Promise<RssSource> => {
    const response = await api.post<RssSource>('/rss-sources', { rssUrl });
    return response.data;
  },

  update: async (id: number, data: {
    blogName?: string;
    author?: string;
    siteUrl?: string;
    crawlUrl?: string;
    language?: string;
    blogType?: string;
  }): Promise<RssSource> => {
    const response = await api.put<RssSource>(`/rss-sources/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/rss-sources/${id}`);
  },

  sync: async (id: number): Promise<SyncResult> => {
    const response = await api.post<SyncResult>(`/rss-sources/${id}/sync`);
    return response.data;
  },

  syncAll: async (): Promise<SyncResult> => {
    const response = await api.post<SyncResult>('/rss-sources/sync-all');
    return response.data;
  },

  importOpml: async (file: File, syncAfterImport = true): Promise<OpmlImportResult> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('syncAfterImport', String(syncAfterImport));

    const response = await api.post<OpmlImportResult>('/rss-sources/import/opml', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  updateTags: async (id: number, tagIds: number[]): Promise<RssSource> => {
    const response = await api.put<RssSource>(`/rss-sources/${id}/tags`, { tagIds });
    return response.data;
  },

  exportOpml: async (): Promise<void> => {
    const response = await api.get('/rss-sources/export/opml', {
      responseType: 'blob',
    });

    // 파일 다운로드 처리
    const blob = new Blob([response.data], { type: 'application/xml' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;

    // Content-Disposition 헤더에서 파일명 추출 또는 기본 파일명 사용
    const contentDisposition = response.headers['content-disposition'];
    let filename = 'feedhub-subscriptions.opml';
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="?([^";\n]+)"?/);
      if (match) {
        filename = match[1];
      }
    }

    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  },

  crawl: async (id: number, mode: 'FULL' | 'INCREMENTAL' = 'FULL'): Promise<CrawlResult> => {
    const response = await api.post<CrawlResult>(`/rss-sources/${id}/crawl?mode=${mode}`);
    return response.data;
  },

  crawlAll: async (mode: 'FULL' | 'INCREMENTAL' = 'FULL'): Promise<CrawlResult[]> => {
    const response = await api.post<CrawlResult[]>(`/rss-sources/crawl-all?mode=${mode}`);
    return response.data;
  },
};

// Tag API
export const tagApi = {
  getAll: async (): Promise<Tag[]> => {
    const response = await api.get<Tag[]>('/tags');
    return response.data;
  },

  create: async (name: string): Promise<Tag> => {
    const response = await api.post<Tag>('/tags', { name });
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/tags/${id}`);
  },

  getSuggestions: async (feedEntryId: number, limit = 10): Promise<TagSuggestion[]> => {
    const response = await api.get<TagSuggestion[]>(`/tags/suggestions?feedEntryId=${feedEntryId}&limit=${limit}`);
    return response.data;
  },
};

// Auth API
export const authApi = {
  signUp: async (data: SignUpRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/signup', data);
    return response.data;
  },

  signIn: async (data: SignInRequest): Promise<AuthResponse> => {
    const response = await api.post<AuthResponse>('/auth/signin', data);
    return response.data;
  },

  checkEmail: async (email: string): Promise<{ available: boolean }> => {
    const response = await api.get<{ available: boolean }>('/auth/check-email', {
      params: { email },
    });
    return response.data;
  },

  getMe: async (): Promise<User> => {
    const response = await api.get<User>('/auth/me');
    return response.data;
  },
};

// QnA API
export const qnaApi = {
  create: async (data: CreateQnaRequest): Promise<Qna> => {
    const response = await api.post<Qna>('/qna', data);
    return response.data;
  },

  getMyList: async (): Promise<Qna[]> => {
    const response = await api.get<Qna[]>('/qna');
    return response.data;
  },

  getAllList: async (): Promise<Qna[]> => {
    const response = await api.get<Qna[]>('/qna/all');
    return response.data;
  },

  getDetail: async (id: number): Promise<QnaDetail> => {
    const response = await api.get<QnaDetail>(`/qna/${id}`);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/qna/${id}`);
  },

  createAnswer: async (qnaId: number, data: CreateAnswerRequest): Promise<QnaAnswer> => {
    const response = await api.post<QnaAnswer>(`/qna/${qnaId}/answer`, data);
    return response.data;
  },
};
