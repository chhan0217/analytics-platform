import { useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse } from "../../api/types";
import { downloadCsv } from "../../utils/csv";

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

  const [startDate, setStartDate] = useState(
    () => new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  );
  const [endDate, setEndDate] = useState(() => new Date().toISOString().split("T")[0]);

  const [screens, setScreens] = useState<ScreenEntry[]>([]);
  const [transitions, setTransitions] = useState<TransitionEntry[]>([]);
  const [funnelSteps, setFunnelSteps] = useState("");
  const [funnelResult, setFunnelResult] = useState<FunnelResponse | null>(null);

  const dateParams = `start_date=${startDate}T00:00:00Z&end_date=${endDate}T23:59:59Z`;

  const loadScreens = async () => {
    const res = await api<DataResponse<{ screens: ScreenEntry[] }>>(
      `/admin/projects/${projectId}/analytics/screens?${dateParams}`,
    );
    setScreens(res.data.screens);
  };

  const loadJourneys = async () => {
    const res = await api<DataResponse<{ transitions: TransitionEntry[] }>>(
      `/admin/projects/${projectId}/analytics/journeys?${dateParams}`,
    );
    setTransitions(res.data.transitions);
  };

  const loadFunnel = async () => {
    const steps = funnelSteps
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean)
      .map((eventKey) => ({ event_key: eventKey }));
    if (steps.length < 2) return;

    const res = await api<DataResponse<FunnelResponse>>(
      `/admin/projects/${projectId}/analytics/funnels/query`,
      {
        method: "POST",
        body: JSON.stringify({
          start_date: `${startDate}T00:00:00Z`,
          end_date: `${endDate}T23:59:59Z`,
          steps,
        }),
      },
    );
    setFunnelResult(res.data);
  };

  const titles: Record<string, string> = {
    screens: "화면 분석",
    journeys: "여정",
    funnels: "퍼널",
  };

  return (
    <div>
      <h2>{titles[tab]}</h2>

      <div style={{ display: "flex", gap: 8, marginBottom: "1.5rem", alignItems: "center" }}>
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
      </div>

      {tab === "screens" && (
        <div>
          <button onClick={() => void loadScreens()} style={btnStyle}>
            조회
          </button>
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
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                <th style={{ padding: 8 }}>화면</th>
                <th style={{ padding: 8 }}>방문 수</th>
                <th style={{ padding: 8 }}>고유 사용자</th>
                <th style={{ padding: 8 }}>평균 체류시간</th>
                <th style={{ padding: 8 }}>이탈 수</th>
                <th style={{ padding: 8 }}>이탈률</th>
              </tr>
            </thead>
            <tbody>
              {screens.map((s) => (
                <tr key={s.screen_id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                  <td style={{ padding: 8 }}>
                    <code>{s.screen_id}</code>
                  </td>
                  <td style={{ padding: 8 }}>{s.view_count}</td>
                  <td style={{ padding: 8 }}>{s.unique_users}</td>
                  <td style={{ padding: 8, color: s.avg_duration_ms ? "#111" : "#ccc" }}>
                    {s.avg_duration_ms ? formatDuration(s.avg_duration_ms) : "-"}
                  </td>
                  <td style={{ padding: 8 }}>{s.bounce_count}</td>
                  <td style={{ padding: 8, color: s.bounce_rate > 0.5 ? "#dc2626" : "#111" }}>
                    {(s.bounce_rate * 100).toFixed(1)}%
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === "journeys" && (
        <div>
          <button onClick={() => void loadJourneys()} style={btnStyle}>
            조회
          </button>
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
