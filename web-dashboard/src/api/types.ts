// 공통 응답
export interface DataResponse<T> {
  data: T;
}

export interface ListResponse<T> {
  data: T[];
  meta: { total: number; page: number; page_size: number };
}

export interface CursorListResponse<T> {
  data: T[];
  meta: { next_cursor: string | null; has_more: boolean };
}

// 인증
export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  admin_user: { id: string; email: string; name: string };
}

// 조직
export interface Organization {
  id: string;
  name: string;
  created_at: string;
}

// 프로젝트
export interface Project {
  id: string;
  organization_id: string;
  name: string;
  unregistered_event_policy: string;
  created_at: string;
}

// 앱
export interface Application {
  id: string;
  project_id: string;
  name: string;
  platform: string;
  app_identifier: string | null;
  created_at: string;
}

// 수집 키
export interface Credential {
  id: string;
  application_id: string;
  key_prefix: string;
  status: string;
  created_at: string;
  revoked_at: string | null;
}

export interface CreatedCredential extends Credential {
  ingestion_key: string;
}

// 텍소노미
export interface TaxonomyEvent {
  id: string;
  project_id: string;
  event_key: string;
  display_name: string;
  description: string | null;
  schema_version: number;
  status: string;
  is_system: boolean;
  created_at: string;
}

export interface TaxonomyProperty {
  id: string;
  taxonomy_event_id: string;
  property_key: string;
  value_type: string;
  required: boolean;
  description: string | null;
}

// 이벤트 집계
export interface EventSummaryResponse {
  events: EventSummaryEntry[];
  period: { start_date: string; end_date: string };
}

export interface EventSummaryEntry {
  event_key: string;
  count: number;
  unique_users: number;
  unique_sessions: number;
}

export interface EventBreakdownResponse {
  event_key: string;
  property_key: string;
  breakdown: BreakdownEntry[];
  period: { start_date: string; end_date: string };
}

export interface BreakdownEntry {
  value: string;
  count: number;
  unique_users: number;
}

// 리텐션
export interface RetentionResponse {
  type: string;
  retention_days: number[];
  cohorts: CohortEntry[];
  period: { start_date: string; end_date: string };
}

export interface CohortEntry {
  date: string;
  cohort_size: number;
  retention: Record<string, { count: number; rate: number }>;
}

// 세션 리플레이
export interface SessionListEntry {
  session_id: string;
  anonymous_id: string;
  external_user_id: string | null;
  started_at: string;
  ended_at: string | null;
  last_event_at: string;
  event_count: number;
  screen_count: number;
  device: DeviceInfo | null;
}

export interface DeviceInfo {
  os_name: string | null;
  os_version: string | null;
  device_model: string | null;
}

export interface SessionReplayResponse {
  session: SessionSummary;
  templates: Record<string, TemplateSnapshot>;
  timeline: TimelineEvent[];
}

export interface TemplateSnapshot {
  viewport: { width: number; height: number };
  wireframe: WireframeNode[];
  screenshot?: string;
}

export interface AppScreenEntry {
  id: string;
  screen_id: string;
  app_version: string;
  viewport_width: number;
  viewport_height: number;
  node_count: number;
  has_screenshot: boolean;
}

export interface ScreenLayoutEntry {
  id: string;
  screen_id: string;
  display_name: string;
  created_at: string;
  updated_at: string;
}

export interface ScreenLayoutDetail {
  id: string;
  screen_id: string;
  display_name: string;
  layout_json: string;
  created_at: string;
  updated_at: string;
}

export interface WireframeNode {
  id: number;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
  text?: string;
  children?: WireframeNode[];
  style?: NodeStyle;
}

export interface NodeStyle {
  background_color?: string;
  text_color?: string;
  font_size?: number;
  corner_radius?: number;
  alpha?: number;
  content_description?: string;
}

export interface SessionSummary {
  session_id: string;
  anonymous_id: string;
  external_user_id: string | null;
  started_at: string;
  ended_at: string | null;
  device: DeviceInfo | null;
}

export interface TimelineEvent {
  event_id: string;
  event_key: string;
  occurred_at: string;
  screen_id: string | null;
  duration_ms: number | null;
  properties: Record<string, string>;
}

// 이벤트
export interface AnalyticsEvent {
  id: string;
  project_id: string;
  application_id: string;
  session_id: string;
  analytics_user_id: string | null;
  anonymous_id: string;
  event_key: string;
  event_schema_version: number;
  properties: string;
  context: string;
  occurred_at: string;
  received_at: string;
  sequence_number: number | null;
}
