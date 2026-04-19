import { useEffect, useState } from 'preact/hooks';
import { api, Field, InstallResult, Schema } from '../api';
import { FieldInput } from '../components/FieldInput';
import { SmsCarriers } from '../components/SmsCarriers';
import { FieldType, Route, SectionHint, routeHref } from '../consts';

interface SubmittedResult {
  result: InstallResult;
  values: Record<string, string>;
  dryRun: boolean;
}

export function Install() {
  const [schema, setSchema] = useState<Schema | null>(null);
  const [values, setValues] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [busy, setBusy] = useState(false);
  const [submitted, setSubmitted] = useState<SubmittedResult | null>(null);
  const [dryRun, setDryRun] = useState(false);

  useEffect(() => {
    Promise.all([api.schema(), api.config()])
      .then(([sc, cfg]) => {
        setSchema(sc);
        const init: Record<string, string> = {};
        for (const section of sc.sections) {
          for (const field of section.fields) {
            init[field.key] = cfg[field.key] ?? field.defaultValue;
          }
        }
        setValues(init);
      })
      .catch((e) => setError(String(e)));
  }, []);

  const onSubmit = async (e: Event) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const r = await api.install(values, dryRun);
      setSubmitted({ result: r, values: { ...values }, dryRun });
    } catch (err) {
      setError(String(err));
    } finally {
      setBusy(false);
    }
  };

  if (error) return <div class="card error">Failed: {error}</div>;
  if (!schema) return <div class="card muted">Loading schema…</div>;

  if (submitted) {
    const { result, values: submittedValues, dryRun: wasDryRun } = submitted;
    const titlePrefix = wasDryRun ? '[dry run] ' : '';
    return (
      <div class="card">
        <h2 class={result.success ? 'success' : 'error'}>
          {titlePrefix}{result.success ? 'Success' : 'Failed'}
        </h2>
        <p>{result.message}</p>
        {wasDryRun && (
          <>
            <h3 style={{ marginTop: 20, marginBottom: 8, fontSize: 15 }}>Supplied values</h3>
            <SuppliedValuesTable schema={schema} values={submittedValues} />
          </>
        )}
        {result.binPath && (
          <details>
            <summary>binPath</summary>
            <pre>{result.binPath}</pre>
          </details>
        )}
        <div class="button-row">
          <a href={routeHref(Route.LAUNCHER)} class="button-link">Back to home</a>
        </div>
      </div>
    );
  }

  const section = schema.sections[activeTab];
  const update = (key: string, v: string) => setValues({ ...values, [key]: v });

  return (
    <form class="card" onSubmit={onSubmit}>
      <div class="tabs">
        {schema.sections.map((s, i) => (
          <button type="button" key={s.title}
                  class={i === activeTab ? 'active' : ''}
                  onClick={() => setActiveTab(i)}>{s.title}</button>
        ))}
      </div>

      {section.hint === SectionHint.SMS_CARRIERS && <SmsCarriers carriers={schema.smsCarriers} />}

      {section.fields.map((field) => (
        <FieldInput key={field.key} field={field}
                    value={values[field.key] ?? ''}
                    onChange={(v) => update(field.key, v)} />
      ))}

      <div class="button-row">
        <label class="dryrun-toggle">
          <input type="checkbox" checked={dryRun}
                 onInput={(e) => setDryRun((e.target as HTMLInputElement).checked)} />
          Dry run
        </label>
        <a href={routeHref(Route.LAUNCHER)} class="button-link">Cancel</a>
        <button type="submit" disabled={busy}>{busy ? 'Installing…' : 'Install'}</button>
      </div>
    </form>
  );
}

function SuppliedValuesTable({ schema, values }: { schema: Schema; values: Record<string, string> }) {
  const fields: Field[] = schema.sections.flatMap((s) => s.fields);
  const rows = fields.map((f) => ({ field: f, value: values[f.key] ?? '' }));
  return (
    <table>
      <thead>
        <tr>
          <th style={{ width: '35%' }}>Field</th>
          <th>Value</th>
        </tr>
      </thead>
      <tbody>
        {rows.map(({ field, value }) => (
          <tr key={field.key}>
            <td>
              <div>{field.label}</div>
              <div class="muted" style={{ fontSize: 11 }}>{field.key}</div>
            </td>
            <td>{renderValue(field, value)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function renderValue(field: Field, value: string) {
  if (value === '') return <span class="muted">(empty — default will apply)</span>;
  if (field.type === FieldType.PASSWORD) return <span class="muted">•••••• ({value.length} chars)</span>;
  return <code style={{ wordBreak: 'break-all' }}>{value}</code>;
}
