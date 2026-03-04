import { useState } from 'react';
import type { FeedEntry } from '../types';
import { buildScrapCommand } from '../utils/scrap';
import './ScrapToolbar.css';

interface ScrapToolbarProps {
  selectedFeeds: FeedEntry[];
  onClearSelection: () => void;
}

export function ScrapToolbar({ selectedFeeds, onClearSelection }: ScrapToolbarProps) {
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copied'>('idle');

  if (selectedFeeds.length === 0) return null;

  const handleScrap = async () => {
    const command = buildScrapCommand(selectedFeeds);
    try {
      await navigator.clipboard.writeText(command);
    } catch {
      // Fallback for older browsers
      const textarea = document.createElement('textarea');
      textarea.value = command;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);
    }
    setCopyStatus('copied');
    setTimeout(() => setCopyStatus('idle'), 3000);
  };

  return (
    <div className="scrap-toolbar">
      {copyStatus === 'copied' ? (
        <span className="scrap-toolbar-message">
          ✅ 클립보드에 복사됨! Claude Code 터미널에 붙여넣기 하세요
        </span>
      ) : (
        <>
          <span className="scrap-toolbar-count">{selectedFeeds.length}개 선택됨</span>
          <button type="button" className="btn-scrap" onClick={handleScrap}>
            Obsidian 스크랩
          </button>
          <button type="button" className="btn-clear-selection" onClick={onClearSelection}>
            선택 해제
          </button>
        </>
      )}
    </div>
  );
}
