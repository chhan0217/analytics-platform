export interface AnalyticsConfig {
  apiKey: string;
  baseUrl: string;
  environment?: string;
  sessionTimeoutMs?: number;
  flushIntervalMs?: number;
  maxBatchSize?: number;
  maxQueueSize?: number;
  errorListener?: (error: AnalyticsError) => void;
}

export interface AnalyticsError {
  code: string;
  message: string;
}
