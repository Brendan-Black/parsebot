import { useEffect } from 'preact/hooks';
import { api } from './api';

const HEARTBEAT_INTERVAL_MS = 3000;

export function useHeartbeat() {
  useEffect(() => {
    let cancelled = false;
    const beat = () => {
      if (cancelled) return;
      api.heartbeat().catch(() => {});
    };
    beat();
    const h = window.setInterval(beat, HEARTBEAT_INTERVAL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(h);
    };
  }, []);
}
