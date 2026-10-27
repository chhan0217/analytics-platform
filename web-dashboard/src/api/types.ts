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
