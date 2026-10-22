import type { AnalyticsConfig } from "./types";

let initialized = false;

export function initialize(_config: AnalyticsConfig): void {
  if (initialized) return;
  initialized = true;
}

export function track(_eventKey: string, _properties: Record<string, unknown> = {}): void {
  checkInitialized();
}

export function trackScreen(_screenId: string, _properties: Record<string, unknown> = {}): void {
  checkInitialized();
}

export function identify(_externalUserId: string): void {
  checkInitialized();
}

export function setUserProperties(_properties: Record<string, unknown>): void {
  checkInitialized();
}

export function resetIdentity(): void {
  checkInitialized();
}

export function flush(): void {
  checkInitialized();
}

function checkInitialized(): void {
  if (!initialized) {
    throw new Error("Analytics SDK가 초기화되지 않았습니다. initialize()를 먼저 호출하세요.");
  }
}
