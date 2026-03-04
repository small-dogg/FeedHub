import type { FeedEntry } from '../types';

// Builds the /scrap command string for the obsidian-scrap Claude Code skill
export function buildScrapCommand(feeds: FeedEntry[]): string {
  const urls = feeds.map((f) => f.link).join(' ');
  return `/obsidian-scrap ${urls}`;
}
