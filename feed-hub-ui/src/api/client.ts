import axios from 'axios';
import type { FeedSlice, FeedSearchParams, OpmlImportResult, RssSource, SyncResult, Tag } from '../types';

const API_BASE = '/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
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
    if (params.lastId !== undefined) {
      searchParams.append('lastId', params.lastId.toString());
    }
    if (params.lastPublishedAt !== undefined) {
      searchParams.append('lastPublishedAt', params.lastPublishedAt);
    }
    if (params.size !== undefined) {
      searchParams.append('size', params.size.toString());
    }
    const response = await api.get<FeedSlice>(`/feeds?${searchParams.toString()}`);
    return response.data;
  },
};

// RSS Source API
export const rssSourceApi = {
  getAll: async (): Promise<RssSource[]> => {
    const response = await api.get<RssSource[]>('/rss-sources');
    return response.data;
  },

  create: async (data: {
    blogName: string;
    author?: string;
    rssUrl: string;
    siteUrl?: string;
    language?: string;
    tagIds?: number[];
  }): Promise<RssSource> => {
    const response = await api.post<RssSource>('/rss-sources', data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/rss-sources/${id}`);
  },

  sync: async (id: number): Promise<SyncResult> => {
    const response = await api.post<SyncResult>(`/rss-sources/${id}/sync`);
    return response.data;
  },

  syncAll: async (): Promise<SyncResult[]> => {
    const response = await api.post<SyncResult[]>('/rss-sources/sync-all');
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
};
