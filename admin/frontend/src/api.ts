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

export interface ModeResponse {
  demo: boolean;
}

export interface EmailAttachment {
  index: number;
  filename: string;
  size: number;
}

export interface EmailMessage {
  index: number;
  subject: string;
  sender: string;
  date: string;
  attachments: EmailAttachment[];
}

export interface EmailPdfsListResponse {
  active: boolean;
  messages: EmailMessage[];
  error: string | null;
}

export interface EmailPdfFetchResponse {
  filename: string;
  pdfBase64: string;
}

export interface SandboxRequest {
  filename: string;
  pdfBase64: string;
  customerCsv: string;
  productCsv: string;
  apiKey: string;
  customRules?: string;
  priceMatrixCsv?: string;
}

export interface SandboxResponse {
  response: string;
  durationMs: number;
}

export type FileChooserMode = 'file' | 'folder';

export interface FileChooserResponse {
  path: string | null;
}

export interface FileReadResponse {
  filename: string;
  base64: string;
  sizeBytes: number;
}

export class ApiError extends Error {
  constructor(message: string, readonly status: number | null, readonly path: string) {
    super(message);
    this.name = 'ApiError';
  }
  toString(): string {
    return this.message;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let r: Response;
  try {
    r = await fetch(path, init);
  } catch (e) {
    throw new ApiError(
      `Could not reach ParseBot admin service at ${path} (${describeNetworkError(e)})`,
      null,
      path
    );
  }
  if (!r.ok) {
    const body = await safeReadText(r);
    throw new ApiError(
      `${r.status} ${r.statusText || ''}`.trim() + (body ? `: ${body}` : '') + ` [${path}]`,
      r.status,
      path
    );
  }
  try {
    return await r.json() as T;
  } catch (e) {
    throw new ApiError(
      `Invalid JSON response from ${path}: ${e instanceof Error ? e.message : String(e)}`,
      r.status,
      path
    );
  }
}

async function safeReadText(r: Response): Promise<string> {
  try {
    const text = (await r.text()).trim();
    return text.length > 500 ? text.slice(0, 500) + '…' : text;
  } catch {
    return '';
  }
}

function describeNetworkError(e: unknown): string {
  if (e instanceof TypeError) return 'service may be stopped or unreachable';
  if (e instanceof Error) return e.message;
  return String(e);
}

function get<T>(path: string): Promise<T> {
  return request<T>(path);
}

function post<T>(path: string, body: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
}

export const api = {
  mode: () => get<ModeResponse>(ApiPath.MODE),
  schema: () => get<Schema>(ApiPath.SCHEMA),
  config: () => get<Record<string, string>>(ApiPath.CONFIG),
  install: (values: Record<string, string>, dryRun: boolean) =>
    post<InstallResult>(ApiPath.SERVICE_INSTALL, { values, dryRun }),
  uninstall: () => post<UninstallResult>(ApiPath.SERVICE_UNINSTALL, {}),
  status: () => get<ServiceStatus>(ApiPath.SERVICE_STATUS),
  events: () => get<EventsResponse>(ApiPath.EVENTS),
  referenceData: () => get<ReferenceDataResponse>(ApiPath.REFERENCE_DATA),
  emailPdfs: () => get<EmailPdfsListResponse>(ApiPath.EMAIL_PDFS),
  emailPdf: (message: number, attachment: number) =>
    get<EmailPdfFetchResponse>(`${ApiPath.EMAIL_PDF}?message=${message}&attachment=${attachment}`),
  sandbox: (req: SandboxRequest) => post<SandboxResponse>(ApiPath.SANDBOX, req),
  fileChooser: (mode: FileChooserMode, initialPath: string, title: string) =>
    post<FileChooserResponse>(ApiPath.FILE_CHOOSER, { mode, initialPath, title }),
  readFile: (path: string) => post<FileReadResponse>(ApiPath.FILE_READ, { path }),
  heartbeat: () => post<{ ok: boolean }>(ApiPath.HEARTBEAT, {}),
  shutdown: () => post<{ ok: boolean }>(ApiPath.SHUTDOWN, {})
};
