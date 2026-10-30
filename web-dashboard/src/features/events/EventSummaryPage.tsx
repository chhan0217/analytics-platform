import { useState } from "react";
import { todayKst, daysAgoKst } from "../../utils/date";
import { api } from "../../api/client";
import { downloadCsv } from "../../utils/csv";
import { useProject } from "../../context/ProjectContext";
import DateRangeFilter from "../../components/DateRangeFilter";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";
import type {
  DataResponse,
  EventSummaryResponse,
  EventSummaryEntry,
  EventBreakdownResponse,
  BreakdownEntry,
  TaxonomyEvent,
  TaxonomyProperty,
  ListResponse,
} from "../../api/types";

export default function EventSummaryPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;

  const [startDate, setStartDate] = useState(() => daysAgoKst(7));
  const [endDate, setEndDate] = useState(() => todayKst());
  const [events, setEvents] = useState<EventSummaryEntry[]>([]);
  const [loaded, setLoaded] = useState(false);

  // 속성별 분석 상태
  const [selectedEventKey, setSelectedEventKey] = useState<string | null>(null);
  const [propertyKeys, setPropertyKeys] = useState<string[]>([]);
  const [selectedPropertyKey, setSelectedPropertyKey] = useState<string | null>(null);
  const [breakdown, setBreakdown] = useState<BreakdownEntry[]>([]);
  const [breakdownLoading, setBreakdownLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadSummary = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api<DataResponse<EventSummaryResponse>>(
        `/admin/projects/${projectId}/analytics/events/summary?start_date=${startDate}&end_date=${endDate}`,
      );
      setEvents(res.data.events);
      setLoaded(true);
      setSelectedEventKey(null);
      setBreakdown([]);
    } catch {
      setError("이벤트 집계를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleEventClick = async (eventKey: string) => {
    setSelectedEventKey(eventKey);
    setBreakdown([]);
    setSelectedPropertyKey(null);

    // 텍소노미에서 해당 이벤트의 속성 목록 조회
    try {
      const taxRes = await api<ListResponse<TaxonomyEvent>>(
        `/admin/projects/${projectId}/taxonomy/events`,
      );
      const taxEvent = taxRes.data.find((e) => e.event_key === eventKey);
      if (taxEvent) {
        const detailRes = await api<
          DataResponse<TaxonomyEvent & { properties: TaxonomyProperty[] }>
        >(`/admin/projects/${projectId}/taxonomy/events/${taxEvent.id}`);
        const props = detailRes.data.properties?.map((p) => p.property_key) || [];
        setPropertyKeys(props);
        if (props.length > 0) {
          setSelectedPropertyKey(props[0]);
          void loadBreakdown(eventKey, props[0]);
        }
      } else {
        setPropertyKeys([]);
      }
    } catch {
      setPropertyKeys([]);
    }
  };

  const loadBreakdown = async (eventKey: string, propertyKey: string) => {
    setBreakdownLoading(true);
    try {
      const res = await api<DataResponse<EventBreakdownResponse>>(
        `/admin/projects/${projectId}/analytics/events/breakdown?start_date=${startDate}&end_date=${endDate}&event_key=${eventKey}&property_key=${propertyKey}`,
      );
      setBreakdown(res.data.breakdown);
    } catch {
      setBreakdown([]);
    }
    setBreakdownLoading(false);
  };

  const handlePropertyChange = (propertyKey: string) => {
    setSelectedPropertyKey(propertyKey);
    if (selectedEventKey) {
      void loadBreakdown(selectedEventKey, propertyKey);
    }
  };

  const totalCount = events.reduce((sum, e) => sum + e.count, 0);

  return (
    <div style={{ display: "flex", gap: "1rem" }}>
      <div style={{ flex: 1 }}>
        <h2>이벤트 집계</h2>
        <DateRangeFilter
          startDate={startDate}
          endDate={endDate}
          onStartDateChange={setStartDate}
          onEndDateChange={setEndDate}
          onSearch={() => void loadSummary()}
          loading={loading}
        >
          {events.length > 0 && (
            <button
              onClick={() =>
                downloadCsv(
                  `events_${startDate}_${endDate}.csv`,
                  ["이벤트 키", "횟수", "사용자 수", "세션 수"],
                  events.map((e) => [e.event_key, e.count, e.unique_users, e.unique_sessions]),
                )
              }
              style={{
                padding: "6px 16px",
                background: "#059669",
                color: "#fff",
                border: "none",
                borderRadius: 6,
                cursor: "pointer",
                fontSize: 14,
              }}
            >
              CSV
            </button>
          )}
        </DateRangeFilter>

        {loading && <LoadingState />}
        {error && <ErrorState message={error} onRetry={() => void loadSummary()} />}
        {loaded && events.length === 0 && !loading && (
          <EmptyState icon="📊" title="이벤트가 없습니다" description="조건을 변경하여 다시 검색해 보세요." />
        )}

        {events.length > 0 && (
          <>
            <p style={{ color: "#666", fontSize: 13, marginBottom: 8 }}>
              총 {totalCount.toLocaleString()}건 / {events.length}종
            </p>
            <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
              <thead>
                <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                  <th style={{ padding: 8 }}>이벤트</th>
                  <th style={{ padding: 8, textAlign: "right" }}>발생 횟수</th>
                  <th style={{ padding: 8, textAlign: "right" }}>사용자</th>
                  <th style={{ padding: 8, textAlign: "right" }}>세션</th>
                </tr>
              </thead>
              <tbody>
                {events.map((evt) => (
                  <tr
                    key={evt.event_key}
                    onClick={() => void handleEventClick(evt.event_key)}
                    style={{
                      borderBottom: "1px solid #f3f4f6",
                      cursor: "pointer",
                      background: selectedEventKey === evt.event_key ? "#e0e7ff" : "transparent",
                    }}
                  >
                    <td style={{ padding: 8 }}>
                      <code>{evt.event_key}</code>
                    </td>
                    <td style={{ padding: 8, textAlign: "right", fontWeight: 600 }}>
                      {evt.count.toLocaleString()}
                    </td>
                    <td style={{ padding: 8, textAlign: "right", color: "#666" }}>
                      {evt.unique_users.toLocaleString()}
                    </td>
                    <td style={{ padding: 8, textAlign: "right", color: "#666" }}>
                      {evt.unique_sessions.toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </div>

      {selectedEventKey && (
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
          <h3>
            <code>{selectedEventKey}</code> 속성별 분석
          </h3>

          {propertyKeys.length === 0 ? (
            <p style={{ color: "#666" }}>텍소노미에 등록된 속성이 없습니다.</p>
          ) : (
            <>
              <div style={{ marginBottom: 12 }}>
                <label style={{ marginRight: 8 }}>속성:</label>
                <select
                  value={selectedPropertyKey || ""}
                  onChange={(e) => handlePropertyChange(e.target.value)}
                  style={{ padding: 6, borderRadius: 4, border: "1px solid #ddd" }}
                >
                  {propertyKeys.map((k) => (
                    <option key={k} value={k}>
                      {k}
                    </option>
                  ))}
                </select>
              </div>

              {breakdownLoading ? (
                <LoadingState />
              ) : breakdown.length === 0 ? (
                <EmptyState icon="📉" title="데이터가 없습니다" />
              ) : (
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                      <th style={{ padding: 6 }}>값</th>
                      <th style={{ padding: 6, textAlign: "right" }}>횟수</th>
                      <th style={{ padding: 6, textAlign: "right" }}>사용자</th>
                    </tr>
                  </thead>
                  <tbody>
                    {breakdown.map((b) => (
                      <tr key={b.value} style={{ borderBottom: "1px solid #f3f4f6" }}>
                        <td style={{ padding: 6 }}>
                          <code>{b.value}</code>
                        </td>
                        <td style={{ padding: 6, textAlign: "right", fontWeight: 600 }}>
                          {b.count.toLocaleString()}
                        </td>
                        <td style={{ padding: 6, textAlign: "right", color: "#666" }}>
                          {b.unique_users.toLocaleString()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
}
