import { useState } from 'preact/hooks';
import { api } from '../api';
import { Route, routeHref } from '../consts';

export function Uninstall() {
  const [busy, setBusy] = useState(false);
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onClick = async () => {
    setBusy(true);
    setError(null);
    try {
      const r = await api.uninstall();
      setResult(r.success
        ? 'Service uninstalled.'
        : `Failed (exit code ${r.exitCode}).`);
    } catch (e) {
      setError(String(e));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div class="card">
      <h2>Uninstall ParseBot Service</h2>
      <p>This will stop the service and remove it from the Windows service registry.</p>
      {result && <p class="success">{result}</p>}
      {error && <p class="error">{error}</p>}
      <div class="button-row">
        <a href={routeHref(Route.LAUNCHER)} class="button-link">Cancel</a>
        <button type="button" class="danger" disabled={busy} onClick={onClick}>
          {busy ? 'Uninstalling…' : 'Uninstall'}
        </button>
      </div>
    </div>
  );
}
