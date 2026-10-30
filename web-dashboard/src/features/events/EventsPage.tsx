import { useState } from "react";
import { todayKst, daysAgoKst, formatKstDateTime } from "../../utils/date";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import DateRangeFilter from "../../components/DateRangeFilter";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";
import type { CursorListResponse, AnalyticsEvent } from "../../api/types";

export default function EventsPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const [events, setEvents] = useState<AnalyticsEvent[]>([]);
  const [cursor, setCursor] = useState<string | null>(null);
  const [hasMore, setHasMore] = useState(false);
  const [startDate, setStartDate] = useState(() => daysAgoKst(7));
  const [endDate, setEndDate] = useState(() => todayKst());
  const [eventKeyFilter, setEventKeyFilter] = useState("");
  const [selectedEvent, setSelectedEvent] = useState<AnalyticsEvent | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async (nextCursor?: string | null) => {
    setLoading(true);
    setError(null);
    try {
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
    } catch {
      setError("이벤트를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ display: "flex", gap: "1rem" }}>
      <div style={{ flex: 1 }}>
        <h2>이벤트 탐색</h2>
        <DateRangeFilter
          startDate={startDate}
          endDate={endDate}
          onStartDateChange={setStartDate}
          onEndDateChange={setEndDate}
          onSearch={() => { setCursor(null); void load(); }}
          loading={loading}
        >
          <input
            value={eventKeyFilter}
            onChange={(e) => setEventKeyFilter(e.target.value)}
            placeholder="event_key 필터"
            style={{ padding: "6px 10px", borderRadius: 6, border: "1px solid #d1d5db", fontSize: 14 }}
          />
        </DateRangeFilter>

        {loading && <LoadingState />}
        {error && <ErrorState message={error} onRetry={() => load()} />}
        {loaded && events.length === 0 && !loading && (
          <EmptyState icon="📋" title="이벤트가 없습니다" description="조건을 변경하여 다시 검색해 보세요." />
        )}

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
                  {formatKstDateTime(evt.occurred_at)}
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
            <strong>발생:</strong> {formatKstDateTime(selectedEvent.occurred_at)}
          </div>
          <div style={{ marginBottom: 8 }}>
            <strong>수신:</strong> {formatKstDateTime(selectedEvent.received_at)}
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
