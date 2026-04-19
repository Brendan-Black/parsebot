import { useEffect, useState } from 'preact/hooks';
import { api } from '../api';

interface SourceLabel {
  loaded: boolean;
  path: string | null;
  error: string | null;
}

export function TestParse() {
  const [filename, setFilename] = useState('');
  const [pdfBase64, setPdfBase64] = useState('');
  const [pdfBytes, setPdfBytes] = useState(0);
  const [customerCsv, setCustomerCsv] = useState('');
  const [productCsv, setProductCsv] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [customRules, setCustomRules] = useState('');
  const [priceMatrixCsv, setPriceMatrixCsv] = useState('');
  const [customerSource, setCustomerSource] = useState<SourceLabel | null>(null);
  const [productSource, setProductSource] = useState<SourceLabel | null>(null);
  const [priceMatrixSource, setPriceMatrixSource] = useState<SourceLabel | null>(null);
  const [busy, setBusy] = useState(false);
  const [response, setResponse] = useState<string | null>(null);
  const [durationMs, setDurationMs] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.config()
      .then((cfg) => {
        if (cfg['claude.api.key']) setApiKey(cfg['claude.api.key']);
      })
      .catch(() => {});

    api.referenceData()
      .then((r) => {
        if (r.customers) {
          setCustomerSource({ loaded: r.customers.content !== null, path: r.customers.path, error: r.customers.error });
          if (r.customers.content) setCustomerCsv(r.customers.content);
        }
        if (r.products) {
          setProductSource({ loaded: r.products.content !== null, path: r.products.path, error: r.products.error });
          if (r.products.content) setProductCsv(r.products.content);
        }
        if (r.priceMatrix) {
          setPriceMatrixSource({ loaded: r.priceMatrix.content !== null, path: r.priceMatrix.path, error: r.priceMatrix.error });
          if (r.priceMatrix.content) setPriceMatrixCsv(r.priceMatrix.content);
        }
      })
      .catch(() => {});
  }, []);

  const onFile = async (e: Event) => {
    const input = e.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    setFilename(file.name);
    const buf = await file.arrayBuffer();
    const bytes = new Uint8Array(buf);
    setPdfBytes(bytes.length);
    setPdfBase64(bytesToBase64(bytes));
  };

  const onSubmit = async (e: Event) => {
    e.preventDefault();
    if (!pdfBase64) { setError('Pick a PDF first'); return; }
    setBusy(true);
    setError(null);
    setResponse(null);
    setDurationMs(null);
    try {
      const r = await api.testParse({
        filename,
        pdfBase64,
        customerCsv,
        productCsv,
        apiKey,
        customRules: customRules || undefined,
        priceMatrixCsv: priceMatrixCsv || undefined
      });
      setResponse(prettyJson(r.response));
      setDurationMs(r.durationMs);
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form class="card" onSubmit={onSubmit}>
      <h2>Test Parse</h2>
      <p class="muted">
        Upload a PDF and send it through the Claude translation pipeline with the given customer and product CSVs.
        Uses the service's configured API key by default.
      </p>

      <div class="field">
        <label>PDF:</label>
        <input type="file" accept="application/pdf" onChange={onFile} />
      </div>
      {filename && <p class="muted">{filename} ({pdfBytes.toLocaleString()} bytes)</p>}

      <div class="field">
        <label>API Key:</label>
        <input type="password" value={apiKey}
               onInput={(e) => setApiKey((e.target as HTMLInputElement).value)} />
      </div>

      <div class="field multiline">
        <label>Customer CSV:{renderSourceLabel(customerSource)}</label>
        <textarea rows={4} value={customerCsv}
                  onInput={(e) => setCustomerCsv((e.target as HTMLTextAreaElement).value)} />
      </div>

      <div class="field multiline">
        <label>Product CSV:{renderSourceLabel(productSource)}</label>
        <textarea rows={4} value={productCsv}
                  onInput={(e) => setProductCsv((e.target as HTMLTextAreaElement).value)} />
      </div>

      <div class="field multiline">
        <label>Custom Rules (optional):</label>
        <textarea rows={3} value={customRules}
                  onInput={(e) => setCustomRules((e.target as HTMLTextAreaElement).value)} />
      </div>

      <div class="field multiline">
        <label>Price Matrix CSV (optional):{renderSourceLabel(priceMatrixSource)}</label>
        <textarea rows={3} value={priceMatrixCsv}
                  onInput={(e) => setPriceMatrixCsv((e.target as HTMLTextAreaElement).value)} />
      </div>

      {error && <p class="error">{error}</p>}

      <div class="button-row">
        <a href="#/launcher" class="button-link">Cancel</a>
        <button type="submit" disabled={busy || !pdfBase64}>
          {busy ? 'Calling Claude…' : 'Send to Claude'}
        </button>
      </div>

      {response && (
        <div style={{ marginTop: 16 }}>
          <h3 class="success">Response{durationMs !== null ? ` (${durationMs} ms)` : ''}</h3>
          <pre style={{
            background: '#f0f0f5',
            padding: 12,
            borderRadius: 4,
            overflow: 'auto',
            fontSize: 12,
            maxHeight: 480
          }}>{response}</pre>
        </div>
      )}
    </form>
  );
}

function renderSourceLabel(source: SourceLabel | null) {
  if (!source) return null;
  const color = source.loaded ? '#3e8e3e' : '#b34c4c';
  const text = source.loaded
    ? `loaded from ${source.path}`
    : source.error ?? 'not loaded';
  return <span style={{ color, fontSize: 12, marginLeft: 6 }}>({text})</span>;
}

function bytesToBase64(bytes: Uint8Array): string {
  const chunk = 0x8000;
  let binary = '';
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode.apply(null, Array.from(bytes.subarray(i, i + chunk)));
  }
  return btoa(binary);
}

function prettyJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}
