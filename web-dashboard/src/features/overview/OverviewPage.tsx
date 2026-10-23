import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse } from "../../api/types";

interface OverviewData {
  total_events: number;
  total_sessions: number;
  total_users: number;
}

interface ScreenItem {
  screen_id: string;
  view_count: number;
  unique_users: number;
}

export default function OverviewPage() {
  const { selectedProject } = useProject();
  const [overview, setOverview] = useState<OverviewData | null>(null);
  const [screens, setScreens] = useState<ScreenItem[]>([]);
  const [days, setDays] = useState(7);

  useEffect(() => {
    if (!selectedProject) return;
    const end = new Date();
    const start = new Date(end.getTime() - days * 24 * 60 * 60 * 1000);
    const params = `start_date=${start.toISOString()}&end_date=${end.toISOString()}`;
    const pid = selectedProject.id;

    api<DataResponse<OverviewData>>(`/admin/projects/${pid}/analytics/overview?${params}`).then(
      (res) => setOverview(res.data),
    );
    api<DataResponse<{ screens: ScreenItem[] }>>(
      `/admin/projects/${pid}/analytics/screens?${params}`,
    ).then((res) => setScreens(res.data.screens));
  }, [selectedProject, days]);

  if (!overview) return <div style={{ color: "#999" }}>로딩 중...</div>;

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

      {/* 핵심 지표 카드 */}
      <div style={{ display: "flex", gap: 16, marginTop: 20 }}>
        <Card label="총 이벤트" value={overview.total_events.toLocaleString()} />
        <Card label="총 세션" value={overview.total_sessions.toLocaleString()} />
        <Card label="총 사용자" value={overview.total_users.toLocaleString()} />
      </div>

      {/* 화면별 통계 */}
      {screens.length > 0 && (
        <div style={{ marginTop: 32 }}>
          <h3>화면별 통계 (상위 10개)</h3>
          <table
            style={{
              width: "100%",
              borderCollapse: "collapse",
              background: "#fff",
              borderRadius: 4,
              overflow: "hidden",
            }}
          >
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

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        flex: 1,
        background: "#fff",
        padding: "1.25rem",
        borderRadius: 8,
        border: "1px solid #e5e7eb",
      }}
    >
      <div style={{ fontSize: 13, color: "#6b7280" }}>{label}</div>
      <div style={{ fontSize: 28, fontWeight: 700, marginTop: 4 }}>{value}</div>
    </div>
  );
}
