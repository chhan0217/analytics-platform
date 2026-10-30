import { useEffect, useState } from "react";
import { todayKst, daysAgoKst } from "../../utils/date";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse } from "../../api/types";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";

interface OverviewPrevious {
  total_events: number;
  total_sessions: number;
  total_users: number;
}

interface OverviewData {
  total_events: number;
  total_sessions: number;
  total_users: number;
  previous?: OverviewPrevious;
}

interface ActiveUsersData {
  dau: number;
  wau: number;
  mau: number;
  stickiness: number;
  previous_dau?: number;
  previous_wau?: number;
  previous_mau?: number;
}

interface ScreenItem {
  screen_id: string;
  view_count: number;
  unique_users: number;
}

export default function OverviewPage() {
  const { selectedProject } = useProject();
  const [overview, setOverview] = useState<OverviewData | null>(null);
  const [activeUsers, setActiveUsers] = useState<ActiveUsersData | null>(null);
  const [screens, setScreens] = useState<ScreenItem[]>([]);
  const [days, setDays] = useState(7);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = async () => {
    if (!selectedProject) return;
    setLoading(true);
    setError(null);
    try {
      const params = `start_date=${daysAgoKst(days)}&end_date=${todayKst()}`;
      const pid = selectedProject.id;

      const [overviewRes, activeUsersRes, screensRes] = await Promise.all([
        api<DataResponse<OverviewData>>(`/admin/projects/${pid}/analytics/overview?${params}`),
        api<DataResponse<ActiveUsersData>>(`/admin/projects/${pid}/analytics/active-users`),
        api<DataResponse<{ screens: ScreenItem[] }>>(
          `/admin/projects/${pid}/analytics/screens?${params}`,
        ),
      ]);
      setOverview(overviewRes.data);
      setActiveUsers(activeUsersRes.data);
      setScreens(screensRes.data.screens);
    } catch {
      setError("개요 데이터를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [selectedProject, days]);

  if (!overview && loading) return <LoadingState />;
  if (!overview && error) return <ErrorState message={error} onRetry={load} />;
  if (!overview) return <LoadingState />;

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h2 style={{ margin: 0 }}>개요</h2>
        <select
          value={days}
          onChange={(e) => setDays(Number(e.target.value))}
          style={{ padding: "4px 8px", borderRadius: 4, border: "1px solid #ddd" }}
        >
          <option value={1}>최근 1일</option>
          <option value={7}>최근 7일</option>
          <option value={30}>최근 30일</option>
        </select>
      </div>

      {error && <ErrorState message={error} onRetry={load} />}

      {/* 활성 사용자 지표 */}
      {activeUsers && (
        <div style={{ display: "flex", gap: 16, marginTop: 20 }}>
          <KpiCard label="DAU (오늘)" value={activeUsers.dau} previous={activeUsers.previous_dau} />
          <KpiCard label="WAU (7일)" value={activeUsers.wau} previous={activeUsers.previous_wau} />
          <KpiCard label="MAU (30일)" value={activeUsers.mau} previous={activeUsers.previous_mau} />
          <KpiCard
            label="스티키니스"
            value={activeUsers.stickiness * 100}
            suffix="%"
            decimals={1}
          />
        </div>
      )}

      {/* 기간 내 집계 */}
      <div style={{ display: "flex", gap: 16, marginTop: 16 }}>
        <KpiCard label="이벤트" value={overview.total_events} previous={overview.previous?.total_events} />
        <KpiCard label="세션" value={overview.total_sessions} previous={overview.previous?.total_sessions} />
        <KpiCard label="사용자" value={overview.total_users} previous={overview.previous?.total_users} />
      </div>

      {/* 화면별 통계 */}
      {screens.length > 0 && (
        <div style={{ marginTop: 32 }}>
          <h3>화면별 통계 (상위 10개)</h3>
          <table style={tableStyle}>
            <thead>
              <tr style={{ background: "#f3f4f6", textAlign: "left", fontSize: 13 }}>
                <th style={{ padding: "8px 12px" }}>화면</th>
                <th style={{ padding: "8px 12px", textAlign: "right" }}>조회수</th>
                <th style={{ padding: "8px 12px", textAlign: "right" }}>사용자</th>
              </tr>
            </thead>
            <tbody>
              {screens.slice(0, 10).map((s) => (
                <tr key={s.screen_id} style={{ borderTop: "1px solid #e5e7eb", fontSize: 13 }}>
                  <td style={{ padding: "8px 12px" }}>
                    <code>{s.screen_id}</code>
                  </td>
                  <td style={{ padding: "8px 12px", textAlign: "right" }}>
                    {s.view_count.toLocaleString()}
                  </td>
                  <td style={{ padding: "8px 12px", textAlign: "right" }}>
                    {s.unique_users.toLocaleString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function KpiCard({
  label,
  value,
  previous,
  suffix = "",
  decimals = 0,
}: {
  label: string;
  value: number;
  previous?: number;
  suffix?: string;
  decimals?: number;
}) {
  const displayValue = decimals > 0 ? value.toFixed(decimals) : value.toLocaleString();
  const change =
    previous !== undefined && previous !== null && previous > 0
      ? ((value - previous) / previous) * 100
      : previous === 0 && value > 0
        ? null // 이전 0 → "NEW"
        : undefined; // 이전 데이터 없음

  const isUp = change !== undefined && change !== null && change > 0;
  const isDown = change !== undefined && change !== null && change < 0;
  const isNew = previous === 0 && value > 0;

  return (
    <div style={cardStyle}>
      <div style={labelStyle}>{label}</div>
      <div style={valueStyle}>
        {displayValue}
        {suffix}
      </div>
      {isNew ? (
        <div style={{ ...changeStyle, color: "#10b981" }}>NEW</div>
      ) : change !== undefined && change !== null ? (
        <div
          style={{
            ...changeStyle,
            color: isUp ? "#10b981" : isDown ? "#ef4444" : "#6b7280",
          }}
        >
          {isUp ? "▲" : isDown ? "▼" : "—"} {Math.abs(change).toFixed(1)}% vs 이전
        </div>
      ) : null}
    </div>
  );
}

const cardStyle: React.CSSProperties = {
  flex: 1,
  background: "#fff",
  padding: "1.25rem",
  borderRadius: 8,
  border: "1px solid #e5e7eb",
};

const labelStyle: React.CSSProperties = {
  fontSize: 13,
  color: "#6b7280",
};

const valueStyle: React.CSSProperties = {
  fontSize: 28,
  fontWeight: 700,
  marginTop: 4,
};

const changeStyle: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  marginTop: 6,
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  background: "#fff",
  borderRadius: 4,
  overflow: "hidden",
};
