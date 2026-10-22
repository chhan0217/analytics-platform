export interface AnalyticsConfig {
  apiKey: string;
  baseUrl: string;
  environment?: string;
  appVersion?: string;
  sessionTimeoutMs?: number;
  flushIntervalMs?: number;
  maxBatchSize?: number;
  maxQueueSize?: number;
  maxRetries?: number;
  errorListener?: (error: AnalyticsError) => void;
}

export interface AnalyticsError {
  code: string;
  message: string;
}

export interface AnalyticsEvent {
  event_id: string;
  session_id: string;
  anonymous_id: string;
  event_key: string;
  schema_version: number;
  occurred_at: string;
  sequence_number?: number;
  properties: Record<string, unknown>;
  context: Record<string, unknown>;
  external_user_id?: string;
}

export interface IngestPayload {
  sdk_info?: { sdk_name: string; sdk_version: string };
  events: AnalyticsEvent[];
}
