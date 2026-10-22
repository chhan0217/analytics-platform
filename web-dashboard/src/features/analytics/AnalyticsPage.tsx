import { useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../../api/client";
import type { DataResponse } from "../../api/types";

interface Overview {
  total_events: number;
  total_sessions: number;
  total_users: number;
}

interface ScreenEntry {
  screen_id: string;
  view_count: number;
  unique_users: number;
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

type Tab = "overview" | "screens" | "journeys" | "funnel";

export default function AnalyticsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [tab, setTab] = useState<Tab>("overview");
  const [startDate, setStartDate] = useState(
    () => new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split("T")[0],
  );
  const [endDate, setEndDate] = useState(() => new Date().toISOString().split("T")[0]);

  const [overview, setOverview] = useState<Overview | null>(null);
  const [screens, setScreens] = useState<ScreenEntry[]>([]);
  const [transitions, setTransitions] = useState<TransitionEntry[]>([]);
  const [funnelSteps, setFunnelSteps] = useState("");
  const [funnelResult, setFunnelResult] = useState<FunnelResponse | null>(null);

  const dateParams = `start_date=${startDate}T00:00:00Z&end_date=${endDate}T23:59:59Z`;

  const loadOverview = async () => {
    const res = await api<DataResponse<Overview>>(
      `/admin/projects/${projectId}/analytics/overview?${dateParams}`,
    );
    setOverview(res.data);
  };

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

  const tabs: { key: Tab; label: string }[] = [
    { key: "overview", label: "개요" },
    { key: "screens", label: "화면 통계" },
    { key: "journeys", label: "여정" },
    { key: "funnel", label: "퍼널" },
  ];

  return (
    <div>
      <h2>분석</h2>

      <div style={{ display: "flex", gap: 8, marginBottom: "1rem", alignItems: "center" }}>
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

      <div style={{ display: "flex", gap: 4, marginBottom: "1.5rem" }}>
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            style={{
              padding: "6px 16px",
              borderRadius: 4,
              border: "1px solid #ddd",
              background: tab === t.key ? "#4f46e5" : "#fff",
              color: tab === t.key ? "#fff" : "#333",
              cursor: "pointer",
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === "overview" && (
        <div>
          <button
            onClick={() => void loadOverview()}
            style={{
              padding: "8px 16px",
              background: "#4f46e5",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              marginBottom: "1rem",
            }}
          >
            조회
          </button>
          {overview && (
            <div style={{ display: "flex", gap: "1rem" }}>
              {[
                { label: "이벤트 수", value: overview.total_events },
                { label: "세션 수", value: overview.total_sessions },
                { label: "사용자 수", value: overview.total_users },
              ].map((item) => (
                <div
                  key={item.label}
                  style={{
                    background: "#fff",
                    border: "1px solid #e5e7eb",
                    borderRadius: 8,
                    padding: "1.5rem 2rem",
                    textAlign: "center",
                    flex: 1,
                  }}
                >
                  <div style={{ fontSize: 32, fontWeight: 700, color: "#4f46e5" }}>
                    {item.value.toLocaleString()}
                  </div>
                  <div style={{ color: "#666", marginTop: 4 }}>{item.label}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === "screens" && (
        <div>
          <button
            onClick={() => void loadScreens()}
            style={{
              padding: "8px 16px",
              background: "#4f46e5",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              marginBottom: "1rem",
            }}
          >
            조회
          </button>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                <th style={{ padding: 8 }}>화면</th>
                <th style={{ padding: 8 }}>방문 수</th>
                <th style={{ padding: 8 }}>고유 사용자</th>
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === "journeys" && (
        <div>
          <button
            onClick={() => void loadJourneys()}
            style={{
              padding: "8px 16px",
              background: "#4f46e5",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              marginBottom: "1rem",
            }}
          >
            조회
          </button>
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

      {tab === "funnel" && (
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
          <button
            onClick={() => void loadFunnel()}
            style={{
              padding: "8px 16px",
              background: "#4f46e5",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              marginBottom: "1rem",
            }}
          >
            분석
          </button>
          {funnelResult && (
            <div>
              <div style={{ marginBottom: "1rem", fontSize: 14, color: "#666" }}>
                전체 전환율: {(funnelResult.overall_conversion_rate * 100).toFixed(1)}%
              </div>
              {funnelResult.steps.map((s) => (
                <div
                  key={s.step}
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 12,
                    marginBottom: 8,
                  }}
                >
                  <div
                    style={{
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
                    }}
                  >
                    {s.step}
                  </div>
                  <div style={{ flex: 1 }}>
                    <code>{s.event_key}</code>
                    <div style={{ fontSize: 13, color: "#666" }}>
                      사용자: {s.unique_users} / 세션: {s.unique_sessions}
                    </div>
                  </div>
                  <div
                    style={{
                      background: "#e0e7ff",
                      padding: "4px 12px",
                      borderRadius: 4,
                      fontSize: 14,
                      fontWeight: 600,
                    }}
                  >
                    {(s.conversion_rate * 100).toFixed(1)}%
                  </div>
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
