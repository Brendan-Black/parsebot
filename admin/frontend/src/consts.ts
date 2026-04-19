export const Route = {
  LAUNCHER: 'launcher',
  INSTALL: 'install',
  UNINSTALL: 'uninstall',
  EVENTS: 'events',
  STATUS: 'status',
  SANDBOX: 'sandbox'
} as const;

export type Route = typeof Route[keyof typeof Route];

export const LEGACY_UI_ROUTE = 'ui';

const ROUTE_VALUES: readonly Route[] = Object.values(Route);

export function isRoute(value: string): value is Route {
  return (ROUTE_VALUES as readonly string[]).includes(value);
}

export function routeHref(route: Route): string {
  return `#/${route}`;
}

export const EventSeverity = {
  CRITICAL: 'CRITICAL',
  INFO: 'INFO',
  AUDIT: 'AUDIT'
} as const;

export type EventSeverity = typeof EventSeverity[keyof typeof EventSeverity];

export const FieldType = {
  TEXT: 'text',
  PASSWORD: 'password',
  FILE: 'file',
  FOLDER: 'folder',
  BOOLEAN: 'boolean',
  TIME: 'time',
  LIST: 'list'
} as const;

export type FieldType = typeof FieldType[keyof typeof FieldType];

export const SectionHint = {
  SMS_CARRIERS: 'sms-carriers'
} as const;

export const ConfigKey = {
  CLAUDE_API_KEY: 'claude.api.key'
} as const;

export const ALL_EVENT_TYPES = 'All';

export const ApiPath = {
  SCHEMA: '/api/schema',
  CONFIG: '/api/config',
  SERVICE_INSTALL: '/api/service/install',
  SERVICE_UNINSTALL: '/api/service/uninstall',
  SERVICE_STATUS: '/api/service/status',
  EVENTS: '/api/events',
  REFERENCE_DATA: '/api/reference-data',
  EMAIL_PDFS: '/api/email-pdfs',
  EMAIL_PDF: '/api/email-pdf',
  MODE: '/api/mode',
  SANDBOX: '/api/sandbox',
  FILE_CHOOSER: '/api/file-chooser',
  FILE_READ: '/api/file-read',
  HEARTBEAT: '/api/heartbeat',
  SHUTDOWN: '/api/shutdown'
} as const;
