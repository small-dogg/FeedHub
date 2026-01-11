import axios from 'axios';
import type { FeedPage, FeedSearchParams, RssSource, Tag } from '../types';

const API_BASE = '/api/v1';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Feed API
export const feedApi = {
  search: async (params: FeedSearchParams): Promise<FeedPage> => {
    const searchParams = new URLSearchParams();
    if (params.rssSourceIds?.length) {
      params.rssSourceIds.forEach(id => searchParams.append('rssSourceIds', id.toString()));
    }
    if (params.tagIds?.length) {
      params.tagIds.forEach(id => searchParams.append('tagIds', id.toString()));
    }
    if (params.page !== undefined) {
      searchParams.append('page', params.page.toString());
    }
    if (params.size !== undefined) {
      searchParams.append('size', params.size.toString());
    }
    const response = await api.get<FeedPage>(`/feeds?${searchParams.toString()}`);
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
