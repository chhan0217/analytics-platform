import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { ListResponse, TaxonomyEvent, TaxonomyProperty, DataResponse } from "../../api/types";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";

export default function TaxonomyPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const [events, setEvents] = useState<TaxonomyEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<TaxonomyEvent | null>(null);
  const [properties, setProperties] = useState<TaxonomyProperty[]>([]);
  const [eventKey, setEventKey] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [propKey, setPropKey] = useState("");
  const [propType, setPropType] = useState("STRING");
  const [propRequired, setPropRequired] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadEvents = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api<ListResponse<TaxonomyEvent>>(
        `/admin/projects/${projectId}/taxonomy/events?page_size=100`,
      );
      setEvents(res.data);
    } catch {
      setError("텍소노미 이벤트를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadProperties = async (eventId: string) => {
    const res = await api<ListResponse<TaxonomyProperty>>(
      `/admin/projects/${projectId}/taxonomy/events/${eventId}/properties`,
    );
    setProperties(res.data);
  };

  useEffect(() => {
    void loadEvents();
  }, [projectId]);

  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!eventKey.trim() || !displayName.trim()) return;
    await api<DataResponse<TaxonomyEvent>>(`/admin/projects/${projectId}/taxonomy/events`, {
      method: "POST",
      body: JSON.stringify({ event_key: eventKey, display_name: displayName }),
    });
    setEventKey("");
    setDisplayName("");
    await loadEvents();
  };

  const handleSelectEvent = async (evt: TaxonomyEvent) => {
    setSelectedEvent(evt);
    await loadProperties(evt.id);
  };

  const handleAddProperty = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEvent || !propKey.trim()) return;
    await api(`/admin/projects/${projectId}/taxonomy/events/${selectedEvent.id}/properties`, {
      method: "POST",
      body: JSON.stringify({ property_key: propKey, value_type: propType, required: propRequired }),
    });
    setPropKey("");
    setPropRequired(false);
    await loadProperties(selectedEvent.id);
  };

  return (
    <div style={{ display: "flex", gap: "1.5rem" }}>
      <div style={{ flex: 1 }}>
        <h2>텍소노미 이벤트</h2>
        <form
          onSubmit={handleCreateEvent}
          style={{ display: "flex", gap: 8, marginBottom: "1rem" }}
        >
          <input
            value={eventKey}
            onChange={(e) => setEventKey(e.target.value)}
            placeholder="event_key (snake_case)"
            style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", flex: 1 }}
          />
          <input
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            placeholder="표시명"
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
            등록
          </button>
        </form>
        {loading && <LoadingState />}
        {error && <ErrorState message={error} onRetry={() => void loadEvents()} />}
        {!loading && !error && events.length === 0 && (
          <EmptyState
            icon="📋"
            title="등록된 이벤트가 없습니다"
            description="위 폼에서 이벤트를 등록해보세요"
          />
        )}
        <div>
          {events.map((evt) => (
            <div
              key={evt.id}
              onClick={() => void handleSelectEvent(evt)}
              style={{
                padding: "0.75rem",
                border: "1px solid #e5e7eb",
                borderRadius: 4,
                marginBottom: 4,
                cursor: "pointer",
                background: selectedEvent?.id === evt.id ? "#e0e7ff" : "#fff",
              }}
            >
              <div style={{ display: "flex", justifyContent: "space-between" }}>
                <code style={{ fontSize: 13 }}>{evt.event_key}</code>
                <div style={{ display: "flex", gap: 4 }}>
                  {evt.is_system && (
                    <span
                      style={{
                        background: "#dbeafe",
                        padding: "1px 6px",
                        borderRadius: 4,
                        fontSize: 11,
                      }}
                    >
                      시스템
                    </span>
                  )}
                  <span
                    style={{
                      background: evt.status === "ACTIVE" ? "#dcfce7" : "#fef3c7",
                      padding: "1px 6px",
                      borderRadius: 4,
                      fontSize: 11,
                    }}
                  >
                    {evt.status}
                  </span>
                </div>
              </div>
              <div style={{ fontSize: 13, color: "#666", marginTop: 4 }}>{evt.display_name}</div>
            </div>
          ))}
        </div>
      </div>

      {selectedEvent && (
        <div style={{ flex: 1 }}>
          <h3>{selectedEvent.display_name} 속성</h3>
          <form
            onSubmit={handleAddProperty}
            style={{ display: "flex", gap: 8, marginBottom: "1rem" }}
          >
            <input
              value={propKey}
              onChange={(e) => setPropKey(e.target.value)}
              placeholder="속성 키"
              style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", flex: 1 }}
            />
            <select
              value={propType}
              onChange={(e) => setPropType(e.target.value)}
              style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
            >
              {["STRING", "INTEGER", "FLOAT", "BOOLEAN", "ARRAY", "OBJECT"].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </select>
            <label style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 13 }}>
              <input
                type="checkbox"
                checked={propRequired}
                onChange={(e) => setPropRequired(e.target.checked)}
              />{" "}
              필수
            </label>
            <button
              type="submit"
              style={{
                padding: "8px 12px",
                background: "#10b981",
                color: "#fff",
                border: "none",
                borderRadius: 4,
                cursor: "pointer",
              }}
            >
              추가
            </button>
          </form>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb" }}>
                <th style={{ padding: 6, textAlign: "left" }}>키</th>
                <th style={{ padding: 6, textAlign: "left" }}>타입</th>
                <th style={{ padding: 6, textAlign: "left" }}>필수</th>
              </tr>
            </thead>
            <tbody>
              {properties.map((p) => (
                <tr key={p.id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                  <td style={{ padding: 6 }}>
                    <code>{p.property_key}</code>
                  </td>
                  <td style={{ padding: 6 }}>{p.value_type}</td>
                  <td style={{ padding: 6 }}>{p.required ? "✓" : ""}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
