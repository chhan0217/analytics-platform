import { useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { CursorListResponse, AnalyticsEvent } from "../../api/types";

export default function EventsPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const [events, setEvents] = useState<AnalyticsEvent[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [startDate, setStartDate] = useState(
    () => new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  );
  const [endDate, setEndDate] = useState(() => new Date().toISOString().split("T")[0]);
  const [eventKeyFilter, setEventKeyFilter] = useState("");
  const [selectedEvent, setSelectedEvent] = useState<AnalyticsEvent | null>(null);
  const [loaded, setLoaded] = useState(false);

  const load = async (nextCursor?: string | null) => {
    let url = `/admin/projects/${projectId}/events?start_date=${startDate}&end_date=${endDate}&page_size=20`;
    if (eventKeyFilter) url += `&event_key=${eventKeyFilter}`;
    if (nextCursor) url += `&cursor=${nextCursor}`;

    const res = await api<CursorListResponse<AnalyticsEvent>>(url);
    if (nextCursor) {
      setEvents((prev) => [...prev, ...res.data]);
    } else {
      setEvents(res.data);
    }
    setCursor(res.meta.next_cursor);
    setHasMore(res.meta.has_more);
    setLoaded(true);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCursor(null);
    void load();
  };

  return (
    <div style={{ display: "flex", gap: "1rem" }}>
      <div style={{ flex: 1 }}>
        <h2>이벤트 탐색</h2>
        <form
          onSubmit={handleSearch}
          style={{ display: "flex", gap: 8, marginBottom: "1rem", flexWrap: "wrap" }}
        >
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
          />
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
          />
          <input
            value={eventKeyFilter}
            onChange={(e) => setEventKeyFilter(e.target.value)}
            placeholder="event_key 필터"
            style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
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
            조회
          </button>
        </form>

        {loaded && events.length === 0 && <p style={{ color: "#666" }}>이벤트가 없습니다.</p>}

        <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
          <thead>
            <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
              <th style={{ padding: 6 }}>이벤트 키</th>
              <th style={{ padding: 6 }}>발생 시각</th>
              <th style={{ padding: 6 }}>세션</th>
              <th style={{ padding: 6 }}>익명 ID</th>
            </tr>
          </thead>
          <tbody>
            {events.map((evt) => (
              <tr
                key={evt.id}
                onClick={() => setSelectedEvent(evt)}
                style={{
                  borderBottom: "1px solid #f3f4f6",
                  cursor: "pointer",
                  background: selectedEvent?.id === evt.id ? "#e0e7ff" : "transparent",
                }}
              >
                <td style={{ padding: 6 }}>
                  <code>{evt.event_key}</code>
                </td>
                <td style={{ padding: 6, color: "#666" }}>
                  {new Date(evt.occurred_at).toLocaleString()}
                </td>
                <td style={{ padding: 6, color: "#666", fontSize: 11 }}>
                  {evt.session_id.slice(0, 8)}...
                </td>
                <td style={{ padding: 6, color: "#666", fontSize: 11 }}>
                  {evt.anonymous_id.slice(0, 8)}...
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {hasMore && (
          <button
            onClick={() => void load(cursor)}
            style={{
              marginTop: "1rem",
              padding: "8px 16px",
              background: "#e5e7eb",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
            }}
          >
            더 보기
          </button>
        )}
      </div>

      {selectedEvent && (
        <div
          style={{
            width: 400,
            background: "#fff",
            border: "1px solid #e5e7eb",
            borderRadius: 8,
            padding: "1rem",
            fontSize: 13,
          }}
        >
          <h3>이벤트 상세</h3>
          <div style={{ marginBottom: 8 }}>
            <strong>ID:</strong> <code style={{ fontSize: 11 }}>{selectedEvent.id}</code>
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>이벤트 키:</strong> {selectedEvent.event_key}
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>발생:</strong> {new Date(selectedEvent.occurred_at).toLocaleString()}
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>수신:</strong> {new Date(selectedEvent.received_at).toLocaleString()}
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>세션:</strong> <code style={{ fontSize: 11 }}>{selectedEvent.session_id}</code>
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>익명 ID:</strong>{" "}
            <code style={{ fontSize: 11 }}>{selectedEvent.anonymous_id}</code>
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>Properties:</strong>
          </div>
          <pre
            style={{
              background: "#f9fafb",
              padding: 8,
              borderRadius: 4,
              overflow: "auto",
              fontSize: 12,
            }}
          >
            {JSON.stringify(JSON.parse(selectedEvent.properties), null, 2)}
          </pre>
          <div style={{ marginBottom: 8, marginTop: 8 }}>
            <strong>Context:</strong>
          </div>
          <pre
            style={{
              background: "#f9fafb",
              padding: 8,
              borderRadius: 4,
              overflow: "auto",
              fontSize: 12,
            }}
          >
            {JSON.stringify(JSON.parse(selectedEvent.context), null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
