export type FieldType =
  | 'text'
  | 'password'
  | 'file'
  | 'folder'
  | 'boolean'
  | 'time'
  | 'list';

export interface Field {
  key: string;
  label: string;
  defaultValue: string;
  type: FieldType;
}

export interface Section {
  title: string;
  fields: Field[];
  hint: string | null;
}

export interface Carrier {
  gateway: string;
  carrier: string;
}

export interface Schema {
  sections: Section[];
  smsCarriers: Carrier[];
}

export interface InstallResult {
  success: boolean;
  message: string;
  binPath: string | null;
}

export interface UninstallResult {
  success: boolean;
  exitCode: number;
}

export interface ServiceStatus {
  installed: boolean;
  running: boolean;
  detail: string;
}

export type EventSeverity = 'CRITICAL' | 'INFO' | 'AUDIT';

export interface ApiEvent {
  id: string;
  type: string;
  severity: EventSeverity;
  timestamp: string;
  message: string;
  details: Record<string, string> | null;
}

export interface EventsResponse {
  logDir: string;
  events: ApiEvent[];
}

export interface ReferenceFile {
  path: string;
  content: string | null;
  error: string | null;
}

export interface ReferenceDataResponse {
  customers: ReferenceFile | null;
  products: ReferenceFile | null;
  priceMatrix: ReferenceFile | null;
}

export interface TestParseRequest {
  filename: string;
  pdfBase64: string;
  customerCsv: string;
  productCsv: string;
  apiKey: string;
  customRules?: string;
  priceMatrixCsv?: string;
}

export interface TestParseResponse {
  response: string;
  durationMs: number;
}

async function get<T>(path: string): Promise<T> {
  const r = await fetch(path);
  if (!r.ok) throw new Error(`${r.status}: ${await r.text()}`);
  return r.json() as Promise<T>;
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const r = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  if (!r.ok) throw new Error(`${r.status}: ${await r.text()}`);
  return r.json() as Promise<T>;
}

export const api = {
  schema: () => get<Schema>('/api/schema'),
  config: () => get<Record<string, string>>('/api/config'),
  install: (values: Record<string, string>, dryRun: boolean) =>
    post<InstallResult>('/api/service/install', { values, dryRun }),
  uninstall: () => post<UninstallResult>('/api/service/uninstall', {}),
  status: () => get<ServiceStatus>('/api/service/status'),
  events: () => get<EventsResponse>('/api/events'),
  referenceData: () => get<ReferenceDataResponse>('/api/reference-data'),
  testParse: (req: TestParseRequest) => post<TestParseResponse>('/api/test-parse', req),
  heartbeat: () => post<{ ok: boolean }>('/api/heartbeat', {}),
  shutdown: () => post<{ ok: boolean }>('/api/shutdown', {})
};
