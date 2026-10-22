import type { AnalyticsConfig, AnalyticsEvent, IngestPayload } from "./types";

const SDK_VERSION = "0.1.0";
const STORAGE_KEY_ANONYMOUS_ID = "analytics_anonymous_id";
const STORAGE_KEY_EXTERNAL_USER_ID = "analytics_external_user_id";
const STORAGE_KEY_SESSION_ID = "analytics_session_id";
const STORAGE_KEY_SESSION_START = "analytics_session_start";
const STORAGE_KEY_LAST_EVENT_AT = "analytics_last_event_at";

let config: AnalyticsConfig | null = null;
let eventQueue: AnalyticsEvent[] = [];
let sequenceNumber = 0;
// eslint-disable-next-line @typescript-eslint/no-unused-vars
let flushTimer: ReturnType<typeof setInterval> | null = null;

export function initialize(cfg: AnalyticsConfig): void {
  if (config) return;
  config = {
    sessionTimeoutMs: 30 * 60 * 1000,
    flushIntervalMs: 30 * 1000,
    maxBatchSize: 100,
    maxQueueSize: 1000,
    maxRetries: 3,
    ...cfg,
  };

  startPeriodicFlush();
  trackInternal("app_open");
}

export function track(eventKey: string, properties: Record<string, unknown> = {}): void {
  checkInitialized();
  trackInternal(eventKey, properties);
}

export function trackScreen(screenId: string, properties: Record<string, unknown> = {}): void {
  checkInitialized();
  trackInternal("screen_view", { screen_id: screenId, ...properties });
}

export function identify(externalUserId: string): void {
  checkInitialized();
  localStorage.setItem(STORAGE_KEY_EXTERNAL_USER_ID, externalUserId);
  trackInternal("identify", { external_user_id: externalUserId });
}

export function setUserProperties(_properties: Record<string, unknown>): void {
  checkInitialized();
}

export function resetIdentity(): void {
  checkInitialized();
  localStorage.removeItem(STORAGE_KEY_EXTERNAL_USER_ID);
}

export function flush(): void {
  checkInitialized();
  void flushInternal();
}

function trackInternal(eventKey: string, properties: Record<string, unknown> = {}): void {
  if (!config) return;

  const sessionId = getOrCreateSessionId();
  const anonymousId = getAnonymousId();
  const now = new Date().toISOString();

  const event: AnalyticsEvent = {
    event_id: crypto.randomUUID(),
    session_id: sessionId,
    anonymous_id: anonymousId,
    event_key: eventKey,
    schema_version: 1,
    occurred_at: now,
    sequence_number: ++sequenceNumber,
    properties,
    context: buildContext(),
    external_user_id: localStorage.getItem(STORAGE_KEY_EXTERNAL_USER_ID) ?? undefined,
  };

  if (eventQueue.length >= (config.maxQueueSize ?? 1000)) {
    eventQueue.shift();
  }
  eventQueue.push(event);
}

function getAnonymousId(): string {
  let id = localStorage.getItem(STORAGE_KEY_ANONYMOUS_ID);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(STORAGE_KEY_ANONYMOUS_ID, id);
  }
  return id;
}

function getOrCreateSessionId(): string {
  const now = Date.now();
  const lastEventAt = Number(localStorage.getItem(STORAGE_KEY_LAST_EVENT_AT) ?? "0");
  const sessionStart = Number(localStorage.getItem(STORAGE_KEY_SESSION_START) ?? "0");
  const timeout = config?.sessionTimeoutMs ?? 30 * 60 * 1000;

  const currentId = localStorage.getItem(STORAGE_KEY_SESSION_ID);
  const isExpired =
    !currentId || now - lastEventAt > timeout || now - sessionStart > 24 * 60 * 60 * 1000;

  if (isExpired) {
    const newId = crypto.randomUUID();
    localStorage.setItem(STORAGE_KEY_SESSION_ID, newId);
    localStorage.setItem(STORAGE_KEY_SESSION_START, String(now));
    localStorage.setItem(STORAGE_KEY_LAST_EVENT_AT, String(now));
    sequenceNumber = 0;
    return newId;
  }

  localStorage.setItem(STORAGE_KEY_LAST_EVENT_AT, String(now));
  return currentId;
}

function buildContext(): Record<string, unknown> {
  return {
    sdk_version: SDK_VERSION,
    app_version: config?.appVersion,
    os_name: "Web",
    locale: navigator.language,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    screen_width: window.screen.width,
    screen_height: window.screen.height,
    browser_name: navigator.userAgent,
  };
}

async function flushInternal(): Promise<void> {
  if (!config || eventQueue.length === 0) return;

  const batchSize = config.maxBatchSize ?? 100;
  const maxRetries = config.maxRetries ?? 3;

  while (eventQueue.length > 0) {
    const batch = eventQueue.slice(0, batchSize);
    const payload: IngestPayload = {
      sdk_info: { sdk_name: "analytics-web", sdk_version: SDK_VERSION },
      events: batch,
    };

    let success = false;
    for (let attempt = 0; attempt < maxRetries; attempt++) {
      try {
        const response = await fetch(`${config.baseUrl}/ingest/events`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "X-Ingestion-Key": config.apiKey,
          },
          body: JSON.stringify(payload),
        });

        if (response.ok) {
          success = true;
          break;
        } else if (response.status >= 400 && response.status < 500) {
          success = true;
          break;
        }
      } catch {
        // 네트워크 오류 시 재시도
      }

      if (attempt < maxRetries - 1) {
        await new Promise((r) => setTimeout(r, 1000 * Math.pow(2, attempt)));
      }
    }

    if (success) {
      eventQueue.splice(0, batch.length);
    } else {
      config.errorListener?.({ code: "FLUSH_ERROR", message: "이벤트 전송 실패" });
      break;
    }
  }
}

function startPeriodicFlush(): void {
  const interval = config?.flushIntervalMs ?? 30000;
  flushTimer = setInterval(() => void flushInternal(), interval);
}

function checkInitialized(): void {
  if (!config) {
    throw new Error("Analytics SDK가 초기화되지 않았습니다. initialize()를 먼저 호출하세요.");
  }
}
