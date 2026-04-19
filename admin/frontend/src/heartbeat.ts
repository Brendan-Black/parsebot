import { useEffect, useState } from 'preact/hooks';
import { api } from './api';

const HEARTBEAT_INTERVAL_MS = 3000;

export function useHeartbeat(): boolean {
  const [connected, setConnected] = useState(true);
  useEffect(() => {
    let cancelled = false;
    const beat = () => {
      if (cancelled) return;
      api.heartbeat()
        .then(() => { if (!cancelled) setConnected(true); })
        .catch(() => { if (!cancelled) setConnected(false); });
    };
    beat();
    const h = window.setInterval(beat, HEARTBEAT_INTERVAL_MS);
    return () => {
      cancelled = true;
      window.clearInterval(h);
    };
  }, []);
  return connected;
}
