import { useEffect, useState } from 'preact/hooks';
import { api, EmailAttachment, EmailMessage } from '../api';
import { ConfigKey, Route, routeHref } from '../consts';

interface SourceLabel {
  loaded: boolean;
  path: string | null;
  error: string | null;
}

interface EmailPdfPick {
  message: number;
  attachment: number;
}

export function TestParse() {
  const [pdfPath, setPdfPath] = useState('');
  const [filename, setFilename] = useState('');
  const [pdfBase64, setPdfBase64] = useState('');
  const [pdfBytes, setPdfBytes] = useState(0);
  const [pdfBrowsing, setPdfBrowsing] = useState(false);
  const [customerCsv, setCustomerCsv] = useState('');
  const [productCsv, setProductCsv] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [customRules, setCustomRules] = useState('');
  const [priceMatrixCsv, setPriceMatrixCsv] = useState('');
  const [customerSource, setCustomerSource] = useState<SourceLabel | null>(null);
  const [productSource, setProductSource] = useState<SourceLabel | null>(null);
  const [priceMatrixSource, setPriceMatrixSource] = useState<SourceLabel | null>(null);
  const [emailActive, setEmailActive] = useState(false);
  const [emailMessages, setEmailMessages] = useState<EmailMessage[]>([]);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [emailPick, setEmailPick] = useState<EmailPdfPick | null>(null);
  const [emailFetching, setEmailFetching] = useState(false);
  const [busy, setBusy] = useState(false);
  const [response, setResponse] = useState<string | null>(null);
  const [durationMs, setDurationMs] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.config()
      .then((cfg) => {
        if (cfg[ConfigKey.CLAUDE_API_KEY]) setApiKey(cfg[ConfigKey.CLAUDE_API_KEY]);
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

    api.emailPdfs()
      .then((r) => {
        setEmailActive(r.active);
        setEmailMessages(r.messages);
        setEmailError(r.error);
      })
      .catch((e) => {
        setEmailError(String(e));
      });
  }, []);

  const onBrowsePdf = async () => {
    setPdfBrowsing(true);
    setError(null);
    try {
      const chosen = await api.fileChooser('file', pdfPath, 'Select PDF');
      if (!chosen.path) return;
      const read = await api.readFile(chosen.path);
      setEmailPick(null);
      setPdfPath(chosen.path);
      setFilename(read.filename);
      setPdfBytes(read.sizeBytes);
      setPdfBase64(read.base64);
    } catch (err) {
      setError(`Failed to read PDF: ${err}`);
    } finally {
      setPdfBrowsing(false);
    }
  };

  const onEmailSelect = async (e: Event) => {
    const value = (e.target as HTMLSelectElement).value;
    if (!value) {
      setEmailPick(null);
      return;
    }
    const [msgStr, attStr] = value.split(':');
    const pick = { message: Number(msgStr), attachment: Number(attStr) };
    setEmailPick(pick);
    setEmailFetching(true);
    setError(null);
    try {
      const r = await api.emailPdf(pick.message, pick.attachment);
      setPdfPath('');
      setFilename(r.filename);
      const bytes = base64ToBytes(r.pdfBase64);
      setPdfBytes(bytes.length);
      setPdfBase64(r.pdfBase64);
    } catch (err) {
      setError(`Failed to fetch PDF from email: ${err}`);
      setEmailPick(null);
    } finally {
      setEmailFetching(false);
    }
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

      {emailActive && (
        <div class="field">
          <label>From mailbox:</label>
          <select
            value={emailPick ? `${emailPick.message}:${emailPick.attachment}` : ''}
            disabled={emailFetching}
            onChange={onEmailSelect}>
            <option value="">— select a PDF from the inbox —</option>
            {emailMessages.flatMap((m) => m.attachments.map((a) => (
              <option value={`${m.index}:${a.index}`}>
                {formatEmailOption(m, a)}
              </option>
            )))}
          </select>
          {emailFetching && <span class="muted" style={{ marginLeft: 8 }}>fetching…</span>}
          {emailError && <p class="error">{emailError}</p>}
          {!emailError && emailMessages.length === 0 && (
            <p class="muted">No messages with PDF attachments in {'INBOX'}.</p>
          )}
        </div>
      )}

      <div class="field">
        <label>PDF:</label>
        <div class="path-picker">
          <input type="text" value={pdfPath} readonly placeholder="(no file selected)" />
          <button type="button" onClick={onBrowsePdf} disabled={pdfBrowsing}>
            {pdfBrowsing ? '…' : 'Browse…'}
          </button>
        </div>
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
        <a href={routeHref(Route.LAUNCHER)} class="button-link">Cancel</a>
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

function base64ToBytes(b64: string): Uint8Array {
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function formatEmailOption(msg: EmailMessage, att: EmailAttachment): string {
  const subject = msg.subject || '(no subject)';
  const sender = msg.sender || '(unknown)';
  return `${att.filename} — ${subject} — ${sender}`;
}

function prettyJson(raw: string): string {
  try {
    return JSON.stringify(JSON.parse(raw), null, 2);
  } catch {
    return raw;
  }
}
