import { useState } from "react";
import { todayKst, daysAgoKst } from "../../utils/date";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse } from "../../api/types";
import { downloadCsv } from "../../utils/csv";
import DateRangeFilter from "../../components/DateRangeFilter";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";

interface ScreenEntry {
  screen_id: string;
  view_count: number;
  unique_users: number;
  avg_duration_ms: number | null;
  bounce_count: number;
  bounce_rate: number;
}

interface TransitionEntry {
  from_screen: string;
  to_screen: string;
  count: number;
}

interface FunnelStep {
  step: number;
  event_key: string;
  unique_users: number;
  unique_sessions: number;
  conversion_rate: number;
}

interface FunnelResponse {
  steps: FunnelStep[];
  overall_conversion_rate: number;
}

function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000);
  if (seconds < 60) return `${seconds}초`;
  const minutes = Math.floor(seconds / 60);
  const remainSec = seconds % 60;
  if (minutes < 60) return `${minutes}분 ${remainSec}초`;
  const hours = Math.floor(minutes / 60);
  const remainMin = minutes % 60;
  return `${hours}시간 ${remainMin}분`;
}

export default function AnalyticsPage({ tab }: { tab: "screens" | "funnels" | "journeys" }) {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;

  const [startDate, setStartDate] = useState(() => daysAgoKst(7));
  const [endDate, setEndDate] = useState(() => todayKst());

  const [screens, setScreens] = useState<ScreenEntry[]>([]);
  const [transitions, setTransitions] = useState<TransitionEntry[]>([]);
  const [funnelSteps, setFunnelSteps] = useState("");
  const [funnelResult, setFunnelResult] = useState<FunnelResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sortKey, setSortKey] = useState<keyof ScreenEntry>("view_count");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("desc");

  const dateParams = `start_date=${startDate}&end_date=${endDate}`;

  const loadScreens = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api<DataResponse<{ screens: ScreenEntry[] }>>(
        `/admin/projects/${projectId}/analytics/screens?${dateParams}`,
      );
      setScreens(res.data.screens);
    } catch {
      setError("화면 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadJourneys = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await api<DataResponse<{ transitions: TransitionEntry[] }>>(
        `/admin/projects/${projectId}/analytics/journeys?${dateParams}`,
      );
      setTransitions(res.data.transitions);
    } catch {
      setError("여정 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadFunnel = async () => {
    const steps = funnelSteps
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
      .map((eventKey) => ({ event_key: eventKey }));
    if (steps.length < 2) return;

    setLoading(true);
    setError(null);
    try {
      const res = await api<DataResponse<FunnelResponse>>(
        `/admin/projects/${projectId}/analytics/funnels/query`,
        {
          method: "POST",
          body: JSON.stringify({
            start_date: startDate,
            end_date: endDate,
            steps,
          }),
        },
      );
      setFunnelResult(res.data);
    } catch {
      setError("퍼널 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const titles: Record<string, string> = {
    screens: "화면 분석",
    journeys: "여정",
    funnels: "퍼널",
  };

  return (
    <div>
      <h2>{titles[tab]}</h2>

      <DateRangeFilter
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
        onSearch={() => {
          if (tab === "screens") void loadScreens();
          else if (tab === "journeys") void loadJourneys();
        }}
        loading={loading}
      />

      {loading && <LoadingState />}
      {error && (
        <ErrorState
          message={error}
          onRetry={() => {
            if (tab === "screens") void loadScreens();
            else if (tab === "journeys") void loadJourneys();
            else void loadFunnel();
          }}
        />
      )}

      {tab === "screens" && (
        <div>
          {!loading && !error && screens.length === 0 && (
            <EmptyState icon="📱" title="화면 데이터가 없습니다" description="조회 버튼을 눌러 데이터를 검색하세요" />
          )}
          {screens.length > 0 && (
            <button
              onClick={() =>
                downloadCsv(
                  `screens_${startDate}_${endDate}.csv`,
                  ["화면", "방문 수", "고유 사용자", "평균 체류시간(ms)", "이탈 수", "이탈률"],
                  screens.map((s) => [
                    s.screen_id,
                    s.view_count,
                    s.unique_users,
                    s.avg_duration_ms ?? "",
                    s.bounce_count,
                    (s.bounce_rate * 100).toFixed(1) + "%",
                  ]),
                )
              }
              style={csvBtnStyle}
            >
              CSV
            </button>
          )}
          <ScreenTable
            screens={screens}
            sortKey={sortKey}
            sortDir={sortDir}
            onSort={(key) => {
              if (sortKey === key) {
                setSortDir((d) => (d === "desc" ? "asc" : "desc"));
              } else {
                setSortKey(key);
                setSortDir("desc");
              }
            }}
          />
        </div>
      )}

      {tab === "journeys" && (
        <div>
          {!loading && !error && transitions.length === 0 && (
            <EmptyState icon="🗺️" title="여정 데이터가 없습니다" description="조회 버튼을 눌러 데이터를 검색하세요" />
          )}
          {transitions.length > 0 && (
            <button
              onClick={() =>
                downloadCsv(
                  `journeys_${startDate}_${endDate}.csv`,
                  ["출발 화면", "도착 화면", "전이 횟수"],
                  transitions.map((t) => [t.from_screen, t.to_screen, t.count]),
                )
              }
              style={csvBtnStyle}
            >
              CSV
            </button>
          )}
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                <th style={{ padding: 8 }}>출발 화면</th>
                <th style={{ padding: 8 }}>도착 화면</th>
                <th style={{ padding: 8 }}>전이 횟수</th>
              </tr>
            </thead>
            <tbody>
              {transitions.map((t, i) => (
                <tr key={i} style={{ borderBottom: "1px solid #f3f4f6" }}>
                  <td style={{ padding: 8 }}>
                    <code>{t.from_screen}</code>
                  </td>
                  <td style={{ padding: 8 }}>
                    <code>{t.to_screen}</code>
                  </td>
                  <td style={{ padding: 8 }}>{t.count}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === "funnels" && (
        <div>
          <div style={{ marginBottom: "1rem" }}>
            <label style={{ display: "block", marginBottom: 4, fontSize: 14 }}>
              이벤트 키 (쉼표 구분, 순서대로)
            </label>
            <input
              value={funnelSteps}
              onChange={(e) => setFunnelSteps(e.target.value)}
              placeholder="screen_view, product_view, add_to_cart, purchase"
              style={{
                padding: 8,
                borderRadius: 4,
                border: "1px solid #ddd",
                width: "100%",
                boxSizing: "border-box",
              }}
            />
          </div>
          <button onClick={() => void loadFunnel()} style={btnStyle}>
            분석
          </button>
          {!loading && !error && !funnelResult && (
            <EmptyState icon="🔍" title="퍼널 데이터가 없습니다" description="이벤트 키를 입력하고 분석 버튼을 눌러주세요" />
          )}
          {funnelResult && (
            <button
              onClick={() =>
                downloadCsv(
                  `funnel_${startDate}_${endDate}.csv`,
                  ["단계", "이벤트 키", "사용자 수", "세션 수", "전환율"],
                  funnelResult.steps.map((s) => [
                    s.step,
                    s.event_key,
                    s.unique_users,
                    s.unique_sessions,
                    (s.conversion_rate * 100).toFixed(1) + "%",
                  ]),
                )
              }
              style={csvBtnStyle}
            >
              CSV
            </button>
          )}
          {funnelResult && (
            <div>
              <div style={{ marginBottom: "1rem", fontSize: 14, color: "#666" }}>
                전체 전환율: {(funnelResult.overall_conversion_rate * 100).toFixed(1)}%
              </div>
              {funnelResult.steps.map((s) => (
                <div
                  key={s.step}
                  style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 8 }}
                >
                  <div style={stepCircle}>{s.step}</div>
                  <div style={{ flex: 1 }}>
                    <code>{s.event_key}</code>
                    <div style={{ fontSize: 13, color: "#666" }}>
                      사용자: {s.unique_users} / 세션: {s.unique_sessions}
                    </div>
                  </div>
                  <div style={rateBadge}>{(s.conversion_rate * 100).toFixed(1)}%</div>
                  <div
                    style={{
                      width: `${Math.max(s.conversion_rate * 200, 4)}px`,
                      height: 8,
                      background: "#4f46e5",
                      borderRadius: 4,
                    }}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

const btnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  cursor: "pointer",
  marginBottom: "1rem",
};

const csvBtnStyle: React.CSSProperties = {
  padding: "8px 16px",
  background: "#059669",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  cursor: "pointer",
  marginBottom: "1rem",
  marginLeft: 8,
};

const stepCircle: React.CSSProperties = {
  width: 30,
  height: 30,
  borderRadius: "50%",
  background: "#4f46e5",
  color: "#fff",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontSize: 14,
  fontWeight: 700,
};

const rateBadge: React.CSSProperties = {
  background: "#e0e7ff",
  padding: "4px 12px",
  borderRadius: 4,
  fontSize: 14,
  fontWeight: 600,
};

/* --- 화면 분석 히트맵 테이블 --- */

function bounceRateStyle(rate: number): React.CSSProperties {
  if (rate < 0.1) return { background: "#f0fdf4", color: "#15803d" };
  if (rate < 0.25) return { background: "#fefce8", color: "#a16207" };
  if (rate < 0.4) return { background: "#fff7ed", color: "#c2410c" };
  return { background: "#fef2f2", color: "#dc2626" };
}

function durationStyle(ms: number | null): React.CSSProperties {
  if (!ms) return { color: "#ccc" };
  const sec = ms / 1000;
  if (sec < 3) return { background: "#fef2f2", color: "#dc2626" };
  if (sec < 10) return { background: "#fefce8", color: "#a16207" };
  if (sec < 120) return { background: "#f0fdf4", color: "#15803d" };
  if (sec < 300) return { background: "#fefce8", color: "#a16207" };
  return { background: "#fef2f2", color: "#dc2626" };
}

type SortableKey = keyof ScreenEntry;

const sortableColumns: { key: SortableKey; label: string }[] = [
  { key: "screen_id", label: "화면" },
  { key: "view_count", label: "방문 수" },
  { key: "unique_users", label: "고유 사용자" },
  { key: "avg_duration_ms", label: "평균 체류시간" },
  { key: "bounce_count", label: "이탈 수" },
  { key: "bounce_rate", label: "이탈률" },
];

function ScreenTable({
  screens,
  sortKey,
  sortDir,
  onSort,
}: {
  screens: ScreenEntry[];
  sortKey: SortableKey;
  sortDir: "asc" | "desc";
  onSort: (key: SortableKey) => void;
}) {
  const sorted = [...screens].sort((a, b) => {
    const va = a[sortKey] ?? 0;
    const vb = b[sortKey] ?? 0;
    if (typeof va === "string" && typeof vb === "string") {
      return sortDir === "desc" ? vb.localeCompare(va) : va.localeCompare(vb);
    }
    return sortDir === "desc" ? Number(vb) - Number(va) : Number(va) - Number(vb);
  });

  const maxView = Math.max(...screens.map((s) => s.view_count), 1);

  return (
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
      <thead>
        <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
          {sortableColumns.map((col) => (
            <th
              key={col.key}
              onClick={() => onSort(col.key)}
              style={{ padding: 8, cursor: "pointer", userSelect: "none", fontSize: 13 }}
            >
              {col.label} {sortKey === col.key ? (sortDir === "desc" ? "▼" : "▲") : ""}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {sorted.map((s) => (
          <tr key={s.screen_id} style={{ borderBottom: "1px solid #f3f4f6" }}>
            <td style={{ padding: 8 }}>
              <code>{s.screen_id}</code>
            </td>
            <td style={{ padding: "8px 8px 8px 0", position: "relative" }}>
              <div
                style={{
                  position: "absolute",
                  left: 0,
                  top: 4,
                  bottom: 4,
                  width: `${(s.view_count / maxView) * 100}%`,
                  background: "#EEF2FF",
                  borderRadius: 4,
                }}
              />
              <span style={{ position: "relative", padding: "0 8px", fontWeight: 500 }}>
                {s.view_count.toLocaleString()}
              </span>
            </td>
            <td style={{ padding: 8 }}>{s.unique_users.toLocaleString()}</td>
            <td
              style={{
                padding: 8,
                borderRadius: 4,
                fontWeight: 500,
                ...durationStyle(s.avg_duration_ms),
              }}
            >
              {s.avg_duration_ms ? formatDuration(s.avg_duration_ms) : "-"}
            </td>
            <td style={{ padding: 8 }}>{s.bounce_count.toLocaleString()}</td>
            <td
              style={{
                padding: "4px 8px",
                borderRadius: 4,
                fontWeight: 600,
                fontSize: 13,
                ...bounceRateStyle(s.bounce_rate),
              }}
            >
              {(s.bounce_rate * 100).toFixed(1)}%
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
