import { useEffect, useMemo, useState } from 'preact/hooks';
import { api, ReleaseInfo, UpdateCheckResponse } from '../api';
import { Route, routeHref } from '../consts';

export function Updates() {
  const [data, setData] = useState<UpdateCheckResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checking, setChecking] = useState(false);
  const [selectedTag, setSelectedTag] = useState<string>('');
  const [applying, setApplying] = useState(false);
  const [applied, setApplied] = useState<string | null>(null);

  const check = async () => {
    setChecking(true);
    setError(null);
    try {
      const r = await api.checkForUpdates();
      setData(r);
      const newest = r.releases.find((x) => x.isNewer) ?? r.releases[0];
      setSelectedTag(newest?.tag ?? '');
    } catch (e) {
      setError(String(e));
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => { check(); }, []);

  const selected: ReleaseInfo | undefined = useMemo(
    () => data?.releases.find((r) => r.tag === selectedTag),
    [data, selectedTag]
  );

  const hasNewer = !!data?.releases.some((r) => r.isNewer);

  const apply = async () => {
    if (!selected) return;
    const confirmed = confirm(
      `Install release ${selected.tag}?\n\n` +
      `The ParseBot service will be stopped, files replaced, and the admin UI will reload automatically when the update completes.`
    );
    if (!confirmed) return;
    setApplying(true);
    setError(null);
    try {
      const r = await api.applyUpdate(selected.tag);
      if (!r.success) {
        setError(r.message);
        setApplying(false);
        return;
      }
      setApplied(r.message);
    } catch (e) {
      setError(String(e));
      setApplying(false);
    }
  };

  if (applied) {
    return (
      <div class="card">
        <h2 class="success">Update starting</h2>
        <p>{applied}</p>
        <p class="muted">
          The admin UI is about to disconnect. A new browser tab will open when the
          update finishes (typically under a minute).
        </p>
      </div>
    );
  }

  return (
    <div class="card">
      <h2>Updates</h2>
      <p class="muted">
        Current version: <code>{data?.currentVersion ?? '…'}</code>
      </p>

      {error && <div class="error" style={{ marginBottom: 12 }}>{error}</div>}

      {data && data.releases.length === 0 && (
        <p class="muted">No releases found in the repository.</p>
      )}

      {data && data.releases.length > 0 && (
        <>
          <p>
            {hasNewer
              ? 'A newer release is available.'
              : 'You are on the latest release. Select an older release below to roll back.'}
          </p>

          <label style={{ display: 'block', marginTop: 12 }}>
            <div style={{ fontSize: 13, marginBottom: 4 }}>Release</div>
            <select
              value={selectedTag}
              onChange={(e) => setSelectedTag((e.target as HTMLSelectElement).value)}
              style={{ width: '100%', padding: 6 }}
              disabled={applying}>
              {data.releases.map((r) => (
                <option key={r.tag} value={r.tag}>
                  {r.tag}
                  {r.isCurrent ? ' — current' : ''}
                  {r.isNewer ? ' — newer' : ''}
                  {r.prerelease ? ' — pre-release' : ''}
                  {r.publishedAt ? `  (${r.publishedAt.slice(0, 10)})` : ''}
                </option>
              ))}
            </select>
          </label>

          {selected && (
            <div style={{ marginTop: 12 }}>
              <div class="muted" style={{ fontSize: 12 }}>
                {formatBytes(selected.zipAssetSize)} · published {selected.publishedAt ?? 'unknown'}
              </div>
              {selected.notes && (
                <details style={{ marginTop: 8 }}>
                  <summary>Release notes</summary>
                  <pre style={{ whiteSpace: 'pre-wrap' }}>{selected.notes}</pre>
                </details>
              )}
            </div>
          )}
        </>
      )}

      <div class="button-row">
        <a href={routeHref(Route.LAUNCHER)} class="button-link">Back to home</a>
        <button type="button" onClick={check} disabled={checking || applying}>
          {checking ? 'Checking…' : 'Check for updates'}
        </button>
        <button
          type="button"
          onClick={apply}
          disabled={!selected || applying || checking}>
          {applying ? 'Installing…' : selected ? `Install ${selected.tag}` : 'Install'}
        </button>
      </div>
    </div>
  );
}

function formatBytes(n: number): string {
  if (!n) return '';
  const units = ['B', 'KB', 'MB', 'GB'];
  let v = n;
  let i = 0;
  while (v >= 1024 && i < units.length - 1) { v /= 1024; i++; }
  return `${v.toFixed(v >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
}
