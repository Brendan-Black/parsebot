import { useEffect, useState } from 'preact/hooks';
import { api, ServiceStatus } from '../api';

export function Status() {
  const [status, setStatus] = useState<ServiceStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    api.status()
      .then(setStatus)
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  if (error) return <div class="card error">{error}</div>;
  if (loading || !status) return <div class="card muted">Loading…</div>;

  const label = !status.installed ? 'Not installed' : status.running ? 'Running' : 'Stopped';
  const cls = !status.installed ? 'muted' : status.running ? 'success' : 'error';

  return (
    <div class="card">
      <h2 class={cls}>{label}</h2>
      <details>
        <summary>Details</summary>
        <pre>{status.detail}</pre>
      </details>
      <div class="button-row">
        <button type="button" onClick={load}>Refresh</button>
      </div>
    </div>
  );
}
