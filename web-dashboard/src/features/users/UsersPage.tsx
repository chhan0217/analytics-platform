import { useState } from "react";
import { api } from "../../api/client";
import type { ListResponse } from "../../api/types";

interface UserResult {
  id: string;
  project_id: string;
  external_user_id: string | null;
  created_at: string;
}

interface SessionResult {
  id: string;
  application_id: string;
  anonymous_id: string;
  started_at: string;
  last_event_at: string;
  ended_at: string | null;
}

interface SessionEvent {
  id: string;
  event_key: string;
  properties: string;
  occurred_at: string;
  sequence_number: number | null;
}

export default function UsersPage() {
  const [projectId, setProjectId] = useState("");
  const [searchType, setSearchType] = useState<"external" | "anonymous">("external");
  const [searchValue, setSearchValue] = useState("");
  const [users, setUsers] = useState<UserResult[]>([]);
  const [sessions, setSessions] = useState<SessionResult[]>([]);
  const [events, setEvents] = useState<SessionEvent[]>([]);
  const [selectedSession, setSelectedSession] = useState<string | null>(null);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!projectId || !searchValue) return;
    setSessions([]);
    setEvents([]);
    setSelectedSession(null);

    const param =
      searchType === "external" ? `external_user_id=${searchValue}` : `anonymous_id=${searchValue}`;
    const res = await api<ListResponse<UserResult>>(`/admin/projects/${projectId}/users?${param}`);
    setUsers(res.data);

    if (res.data.length > 0) {
      const sessRes = await api<ListResponse<SessionResult>>(
        `/admin/projects/${projectId}/users/${res.data[0].id}/sessions`,
      );
      setSessions(sessRes.data);
    }
  };

  const handleSelectSession = async (sessionId: string) => {
    setSelectedSession(sessionId);
    const res = await api<ListResponse<SessionEvent>>(
      `/admin/projects/${projectId}/sessions/${sessionId}/events`,
    );
    setEvents(res.data);
  };

  return (
    <div>
      <h2>사용자 검색</h2>
      <form
        onSubmit={handleSearch}
        style={{ display: "flex", gap: 8, marginBottom: "1.5rem", flexWrap: "wrap" }}
      >
        <input
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          placeholder="프로젝트 ID"
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", width: 320 }}
        />
        <select
          value={searchType}
          onChange={(e) => setSearchType(e.target.value as "external" | "anonymous")}
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
        >
          <option value="external">External User ID</option>
          <option value="anonymous">Anonymous ID</option>
        </select>
        <input
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          placeholder="검색값"
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", flex: 1 }}
        />
        <button
          type="submit"
          style={{
            padding: "8px 16px",
            background: "#4f46e5",
            color: "#fff",
            border: "none",
            borderRadius: 4,
            cursor: "pointer",
          }}
        >
          검색
        </button>
      </form>

      {users.length > 0 && (
        <div style={{ marginBottom: "1.5rem" }}>
          <h3>사용자 정보</h3>
          {users.map((u) => (
            <div
              key={u.id}
              style={{
                background: "#fff",
                padding: "0.75rem",
                border: "1px solid #e5e7eb",
                borderRadius: 4,
                marginBottom: 4,
                fontSize: 13,
              }}
            >
              <div>
                <strong>ID:</strong> <code>{u.id}</code>
              </div>
              {u.external_user_id && (
                <div>
                  <strong>External:</strong> {u.external_user_id}
                </div>
              )}
              <div>
                <strong>생성일:</strong> {new Date(u.created_at).toLocaleString()}
              </div>
            </div>
          ))}
        </div>
      )}

      {sessions.length > 0 && (
        <div style={{ display: "flex", gap: "1rem" }}>
          <div style={{ width: 300 }}>
            <h3>세션 목록</h3>
            {sessions.map((s) => (
              <div
                key={s.id}
                onClick={() => void handleSelectSession(s.id)}
                style={{
                  padding: "0.5rem",
                  border: "1px solid #e5e7eb",
                  borderRadius: 4,
                  marginBottom: 4,
                  cursor: "pointer",
                  background: selectedSession === s.id ? "#e0e7ff" : "#fff",
                  fontSize: 13,
                }}
              >
                <div>
                  <code>{s.id.slice(0, 8)}...</code>
                </div>
                <div style={{ color: "#666" }}>{new Date(s.started_at).toLocaleString()}</div>
              </div>
            ))}
          </div>

          {selectedSession && events.length > 0 && (
            <div style={{ flex: 1 }}>
              <h3>이벤트 타임라인</h3>
              {events.map((evt) => (
                <div
                  key={evt.id}
                  style={{
                    display: "flex",
                    gap: 12,
                    padding: "0.5rem 0",
                    borderBottom: "1px solid #f3f4f6",
                    fontSize: 13,
                  }}
                >
                  <div style={{ width: 140, color: "#666" }}>
                    {new Date(evt.occurred_at).toLocaleTimeString()}
                  </div>
                  <div>
                    <code style={{ background: "#e0e7ff", padding: "1px 6px", borderRadius: 4 }}>
                      {evt.event_key}
                    </code>
                    {evt.sequence_number != null && (
                      <span style={{ marginLeft: 8, color: "#999", fontSize: 11 }}>
                        #{evt.sequence_number}
                      </span>
                    )}
                    <pre style={{ margin: "4px 0 0", fontSize: 11, color: "#666" }}>
                      {JSON.stringify(JSON.parse(evt.properties), null, 2)}
                    </pre>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
