import { useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import { todayKst, daysAgoKst } from "../../utils/date";
import { api } from "../../api/client";
import { downloadCsv } from "../../utils/csv";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse } from "../../api/types";
import DateRangeFilter from "../../components/DateRangeFilter";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";

interface TrendDataPoint {
  date: string;
  event_count: number;
  session_count: number;
  user_count: number;
}

interface TrendResponse {
  granularity: string;
  data_points: TrendDataPoint[];
}

type MetricKey = "event_count" | "session_count" | "user_count";

const metricLabels: Record<MetricKey, string> = {
  event_count: "이벤트",
  session_count: "세션",
  user_count: "사용자",
};

const metricColors: Record<MetricKey, string> = {
  event_count: "#4f46e5",
  session_count: "#0891b2",
  user_count: "#059669",
};

interface ChartRow {
  label: string;
  current: number;
  previous?: number;
}

export default function TrendPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;

  const [startDate, setStartDate] = useState(() => daysAgoKst(7));
  const [endDate, setEndDate] = useState(() => todayKst());
  const [granularity, setGranularity] = useState("daily");
  const [eventKey, setEventKey] = useState("");
  const [propertyName, setPropertyName] = useState("");
  const [propertyValue, setPropertyValue] = useState("");
  const [metric, setMetric] = useState<MetricKey>("event_count");
  const [compare, setCompare] = useState(false);
  const [chartData, setChartData] = useState<ChartRow[]>([]);
  const [summary, setSummary] = useState<{ current: number; previous: number } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const buildParams = (start: string, end: string) => {
    const params = new URLSearchParams({
      start_date: start,
      end_date: end,
      granularity,
    });
    if (eventKey.trim()) params.set("event_key", eventKey.trim());
    if (propertyName.trim() && propertyValue.trim()) {
      params.set("property_name", propertyName.trim());
      params.set("property_value", propertyValue.trim());
    }
    return params;
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const currentParams = buildParams(startDate, endDate);
      const currentRes = await api<DataResponse<TrendResponse>>(
        `/admin/projects/${projectId}/analytics/trend?${currentParams}`,
      );
      const currentPoints = currentRes.data.data_points;

      if (compare) {
        const startMs = new Date(startDate).getTime();
        const endMs = new Date(endDate).getTime();
        const diff = endMs - startMs + 24 * 60 * 60 * 1000;
        const prevEnd = new Date(startMs - 24 * 60 * 60 * 1000).toISOString().split("T")[0];
        const prevStart = new Date(startMs - diff).toISOString().split("T")[0];

        const prevParams = buildParams(prevStart, prevEnd);
        const prevRes = await api<DataResponse<TrendResponse>>(
          `/admin/projects/${projectId}/analytics/trend?${prevParams}`,
        );
        const prevPoints = prevRes.data.data_points;

        const rows: ChartRow[] = currentPoints.map((cp, i) => ({
          label: granularity === "hourly" ? cp.date.substring(5) + "h" : cp.date.substring(5),
          current: cp[metric],
          previous: prevPoints[i]?.[metric] ?? 0,
        }));
        setChartData(rows);

        const curTotal = currentPoints.reduce((s, p) => s + p[metric], 0);
        const prevTotal = prevPoints.reduce((s, p) => s + p[metric], 0);
        setSummary({ current: curTotal, previous: prevTotal });
      } else {
        const rows: ChartRow[] = currentPoints.map((cp) => ({
          label: granularity === "hourly" ? cp.date.substring(5) + "h" : cp.date.substring(5),
          current: cp[metric],
        }));
        setChartData(rows);
        setSummary(null);
      }
    } catch {
      setError("데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const changeRate =
    summary && summary.previous > 0
      ? ((summary.current - summary.previous) / summary.previous) * 100
      : null;

  return (
    <div>
      <h2>시계열 추이</h2>

      <DateRangeFilter
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
        onSearch={() => void load()}
        loading={loading}
      >
        <select value={granularity} onChange={(e) => setGranularity(e.target.value)} style={selectStyle}>
          <option value="daily">일별</option>
          <option value="hourly">시간별</option>
        </select>
        <select value={metric} onChange={(e) => setMetric(e.target.value as MetricKey)} style={selectStyle}>
          {Object.entries(metricLabels).map(([key, label]) => (
            <option key={key} value={key}>
              {label}
            </option>
          ))}
        </select>
        <label style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 14 }}>
          <input type="checkbox" checked={compare} onChange={(e) => setCompare(e.target.checked)} />
          기간 비교
        </label>
      </DateRangeFilter>

      <div style={{ display: "flex", gap: 8, marginBottom: "1rem", flexWrap: "wrap", alignItems: "center" }}>
        <input
          value={eventKey}
          onChange={(e) => setEventKey(e.target.value)}
          placeholder="이벤트 키 (선택)"
          style={{ ...selectStyle, width: 180 }}
        />
        <input
          value={propertyName}
          onChange={(e) => setPropertyName(e.target.value)}
          placeholder="속성 이름 (선택)"
          style={{ ...selectStyle, width: 150 }}
        />
        <input
          value={propertyValue}
          onChange={(e) => setPropertyValue(e.target.value)}
          placeholder="속성 값 (선택)"
          style={{ ...selectStyle, width: 150 }}
        />
        {chartData.length > 0 && (
          <button
            onClick={() =>
              downloadCsv(
                `trend_${startDate}_${endDate}.csv`,
                compare ? ["날짜", "이번 기간", "이전 기간"] : ["날짜", metricLabels[metric]],
                chartData.map((r) =>
                  compare ? [r.label, r.current, r.previous ?? 0] : [r.label, r.current],
                ),
              )
            }
            style={csvBtnStyle}
          >
            CSV
          </button>
        )}
      </div>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} onRetry={() => void load()} />}
      {!loading && !error && chartData.length === 0 && (
        <EmptyState icon="📈" title="추이 데이터가 없습니다" description="조회 버튼을 눌러 데이터를 검색하세요" />
      )}

      {summary && (
        <div style={{ display: "flex", gap: 16, marginBottom: "1rem" }}>
          <div style={summaryCard}>
            <div style={{ fontSize: 12, color: "#6b7280" }}>이번 기간</div>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{summary.current.toLocaleString()}</div>
          </div>
          <div style={summaryCard}>
            <div style={{ fontSize: 12, color: "#6b7280" }}>이전 기간</div>
            <div style={{ fontSize: 20, fontWeight: 700 }}>{summary.previous.toLocaleString()}</div>
          </div>
          {changeRate !== null && (
            <div style={summaryCard}>
              <div style={{ fontSize: 12, color: "#6b7280" }}>변화율</div>
              <div
                style={{
                  fontSize: 20,
                  fontWeight: 700,
                  color: changeRate >= 0 ? "#059669" : "#dc2626",
                }}
              >
                {changeRate >= 0 ? "+" : ""}
                {changeRate.toFixed(1)}%
              </div>
            </div>
          )}
        </div>
      )}

      {chartData.length > 0 && (
        <div
          style={{
            background: "#fff",
            borderRadius: 8,
            padding: "1rem",
            boxShadow: "0 1px 3px rgba(0,0,0,0.08)",
          }}
        >
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
              <XAxis dataKey="label" fontSize={12} />
              <YAxis fontSize={12} />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="current"
                name={compare ? `이번 기간 (${metricLabels[metric]})` : metricLabels[metric]}
                stroke={metricColors[metric]}
                strokeWidth={2}
                dot={{ r: 3 }}
                activeDot={{ r: 5 }}
              />
              {compare && (
                <Line
                  type="monotone"
                  dataKey="previous"
                  name={`이전 기간 (${metricLabels[metric]})`}
                  stroke={metricColors[metric]}
                  strokeWidth={2}
                  strokeDasharray="5 5"
                  strokeOpacity={0.5}
                  dot={{ r: 2 }}
                />
              )}
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  );
}

const selectStyle: React.CSSProperties = {
  padding: "6px 10px",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  fontSize: 14,
};

const csvBtnStyle: React.CSSProperties = {
  padding: "6px 16px",
  background: "#059669",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 14,
};

const summaryCard: React.CSSProperties = {
  flex: 1,
  background: "#fff",
  padding: "0.75rem 1rem",
  borderRadius: 8,
  border: "1px solid #e5e7eb",
};
