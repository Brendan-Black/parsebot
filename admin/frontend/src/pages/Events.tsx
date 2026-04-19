import { useEffect, useMemo, useState } from 'preact/hooks';
import { api, ApiEvent } from '../api';

export function Events() {
  const [all, setAll] = useState<ApiEvent[]>([]);
  const [logDir, setLogDir] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [type, setType] = useState('All');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');

  useEffect(() => {
    api.events()
      .then((r) => {
        setAll(r.events);
        setLogDir(r.logDir);
        if (r.events.length > 0) {
          const sorted = [...r.events].sort((a, b) => a.timestamp.localeCompare(b.timestamp));
          setFrom(toLocal(sorted[0].timestamp));
          setTo(toLocal(sorted[sorted.length - 1].timestamp));
        }
      })
      .catch((e) => setError(String(e)));
  }, []);

  const types = useMemo(() => {
    const s = new Set<string>();
    all.forEach((e) => s.add(e.type));
    return Array.from(s).sort();
  }, [all]);

  const filtered = useMemo(() => {
    const fromMs = from ? new Date(from).getTime() : -Infinity;
    const toMs = to ? new Date(to).getTime() : Infinity;
    return all.filter((e) => {
      if (type !== 'All' && e.type !== type) return false;
      const ts = new Date(e.timestamp).getTime();
      return ts >= fromMs && ts <= toMs;
    });
  }, [all, type, from, to]);

  if (error) return <div class="card error">Failed to load events: {error}</div>;

  return (
    <div class="card">
      <div class="filters">
        <label>Type:</label>
        <select value={type} onInput={(e) => setType((e.target as HTMLSelectElement).value)}>
          <option value="All">All</option>
          {types.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
        <label>From:</label>
        <input type="datetime-local" value={from} onInput={(e) => setFrom((e.target as HTMLInputElement).value)} />
        <label>To:</label>
        <input type="datetime-local" value={to} onInput={(e) => setTo((e.target as HTMLInputElement).value)} />
        <button type="button" class="secondary" onClick={() => { setType('All'); setFrom(''); setTo(''); }}>Reset</button>
      </div>

      <p class="muted">Logs: {logDir} — Showing {filtered.length} of {all.length} events</p>

      {all.length === 0 ? (
        <p class="muted">No events found.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Timestamp</th><th>Type</th><th>Severity</th><th>Message</th><th>Details</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((e) => (
              <tr key={e.id} class={e.severity === 'CRITICAL' ? 'critical' : ''}>
                <td>{formatTs(e.timestamp)}</td>
                <td>{e.type}</td>
                <td>{e.severity}</td>
                <td>{e.message}</td>
                <td>{formatDetails(e.details)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function toLocal(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function formatTs(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function formatDetails(details: Record<string, string> | null): string {
  if (!details) return '';
  return Object.entries(details).map(([k, v]) => `${k}=${v}`).join('; ');
}
