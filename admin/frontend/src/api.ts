import { ApiPath, EventSeverity, FieldType } from './consts';

export { EventSeverity, FieldType };

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
  schema: () => get<Schema>(ApiPath.SCHEMA),
  config: () => get<Record<string, string>>(ApiPath.CONFIG),
  install: (values: Record<string, string>, dryRun: boolean) =>
    post<InstallResult>(ApiPath.SERVICE_INSTALL, { values, dryRun }),
  uninstall: () => post<UninstallResult>(ApiPath.SERVICE_UNINSTALL, {}),
  status: () => get<ServiceStatus>(ApiPath.SERVICE_STATUS),
  events: () => get<EventsResponse>(ApiPath.EVENTS),
  referenceData: () => get<ReferenceDataResponse>(ApiPath.REFERENCE_DATA),
  testParse: (req: TestParseRequest) => post<TestParseResponse>(ApiPath.TEST_PARSE, req),
  heartbeat: () => post<{ ok: boolean }>(ApiPath.HEARTBEAT, {}),
  shutdown: () => post<{ ok: boolean }>(ApiPath.SHUTDOWN, {})
};
