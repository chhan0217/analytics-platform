import { useState, useEffect, useCallback, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse, SessionReplayResponse, TimelineEvent } from "../../api/types";
import WireframeRenderer from "./components/WireframeRenderer";

function formatTime(utcStr: string): string {
  return new Date(utcStr).toLocaleTimeString("ko-KR", { timeZone: "Asia/Seoul" });
}

function formatDurationMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const sec = Math.round(ms / 1000);
  if (sec < 60) return `${sec}초`;
  const min = Math.floor(sec / 60);
  return `${min}분 ${sec % 60}초`;
}

function eventLabel(e: TimelineEvent): string {
  switch (e.event_key) {
    case "screen_view":
      return `${e.screen_id || "unknown"}`;
    case "screen_leave":
      return `${e.screen_id || ""} 떠남`;
    case "session_start":
      return "세션 시작";
    case "session_end":
      return "세션 종료";
    case "app_open":
      return "앱 열림";
    default: {
      const props = Object.entries(e.properties)
        .slice(0, 2)
        .map(([k, v]) => `${k}=${v}`)
        .join(", ");
      return props ? `${e.event_key} (${props})` : e.event_key;
    }
  }
}

const EVENT_COLORS: Record<string, string> = {
  screen_view: "#4f46e5",
  screen_leave: "#9ca3af",
  session_start: "#10b981",
  session_end: "#ef4444",
  app_open: "#10b981",
};

export default function SessionReplayPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const navigate = useNavigate();

  const [data, setData] = useState<SessionReplayResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 재생 상태
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const screenEvents = data?.timeline.filter((e) => e.event_key === "screen_view") || [];
  const currentScreen = screenEvents[currentIndex];

  useEffect(() => {
    if (!projectId || !sessionId) return;
    setLoading(true);
    api<DataResponse<SessionReplayResponse>>(
      `/admin/projects/${projectId}/sessions/${sessionId}/replay`,
    )
      .then((res) => setData(res.data))
      .catch(() => setError("세션 데이터를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [projectId, sessionId]);

  // 재생 타이머
  useEffect(() => {
    if (!isPlaying || !currentScreen) return;
    const durationMs = currentScreen.duration_ms || 3000;
    timerRef.current = setTimeout(() => {
      if (currentIndex < screenEvents.length - 1) {
        setCurrentIndex((prev) => prev + 1);
      } else {
        setIsPlaying(false);
      }
    }, durationMs / speed);
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [isPlaying, currentIndex, speed, currentScreen, screenEvents.length]);

  const play = useCallback(() => setIsPlaying(true), []);
  const pause = useCallback(() => setIsPlaying(false), []);
  const prev = useCallback(() => {
    setIsPlaying(false);
    setCurrentIndex((i) => Math.max(0, i - 1));
  }, []);
  const next = useCallback(() => {
    setIsPlaying(false);
    setCurrentIndex((i) => Math.min(screenEvents.length - 1, i + 1));
  }, [screenEvents.length]);

  if (loading) return <p style={{ color: "#9ca3af" }}>로딩 중...</p>;
  if (error) return <p style={{ color: "#ef4444" }}>{error}</p>;
  if (!data) return null;

  const { session, timeline } = data;

  return (
    <div>
      {/* 헤더 */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16 }}>
        <button onClick={() => navigate("/sessions")} style={backBtn}>
          &larr; 세션 목록
        </button>
        <h2 style={{ fontSize: 18, fontWeight: 600, color: "#1e1b4b", margin: 0 }}>
          세션 리플레이
        </h2>
        <span style={{ fontSize: 12, color: "#9ca3af" }}>
          {session.anonymous_id.slice(0, 8)}...
          {session.external_user_id && ` (${session.external_user_id})`}
        </span>
      </div>

      <div style={{ display: "flex", gap: 16, height: "calc(100vh - 200px)" }}>
        {/* 좌측: 화면 + 컨트롤 */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column" }}>
          {/* 현재 화면 카드 */}
          <div style={screenCard}>
            <div style={screenHeader}>
              <span style={screenLabel}>{currentScreen?.screen_id || "-"}</span>
              {currentScreen?.duration_ms && (
                <span style={durationLabel}>{formatDurationMs(currentScreen.duration_ms)}</span>
              )}
            </div>
            <div style={screenBody}>
              {currentScreen?.screen_id && data?.templates[currentScreen.screen_id]?.screenshot ? (
                <img
                  src={data.templates[currentScreen.screen_id].screenshot}
                  alt={currentScreen.screen_id}
                  style={{ maxHeight: 500, borderRadius: 8 }}
                />
              ) : currentScreen?.screen_id && data?.templates[currentScreen.screen_id] ? (
                <WireframeRenderer
                  snapshot={data.templates[currentScreen.screen_id]}
                  maxHeight={500}
                />
              ) : (
                <div style={screenPlaceholder}>
                  <div style={{ fontSize: 40, marginBottom: 8, opacity: 0.3 }}>
                    {currentIndex + 1} / {screenEvents.length}
                  </div>
                  <div style={{ fontSize: 16, fontWeight: 600, color: "#4f46e5" }}>
                    {currentScreen?.screen_id || "화면 없음"}
                  </div>
                  <div style={{ fontSize: 12, color: "#9ca3af", marginTop: 4 }}>
                    와이어프레임 데이터 없음
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 타임라인 바 */}
          <div style={timelineBar}>
            {screenEvents.map((e, i) => (
              <button
                key={e.event_id}
                onClick={() => {
                  setIsPlaying(false);
                  setCurrentIndex(i);
                }}
                style={{
                  ...timelineDot,
                  background: i === currentIndex ? "#4f46e5" : "#d1d5db",
                  transform: i === currentIndex ? "scale(1.4)" : "scale(1)",
                }}
                title={e.screen_id || ""}
              />
            ))}
          </div>

          {/* 화면 이름 라벨 */}
          <div style={screenNamesRow}>
            {screenEvents.map((e, i) => (
              <span
                key={e.event_id}
                style={{
                  fontSize: 10,
                  color: i === currentIndex ? "#4f46e5" : "#9ca3af",
                  fontWeight: i === currentIndex ? 600 : 400,
                  textAlign: "center",
                  flex: 1,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {e.screen_id || "?"}
              </span>
            ))}
          </div>

          {/* 재생 컨트롤 */}
          <div style={controlBar}>
            <button onClick={prev} style={controlBtn}>
              &#9664;
            </button>
            <button onClick={isPlaying ? pause : play} style={playBtn}>
              {isPlaying ? "||" : "\u25B6"}
            </button>
            <button onClick={next} style={controlBtn}>
              &#9654;
            </button>
            <select
              value={speed}
              onChange={(e) => setSpeed(Number(e.target.value))}
              style={speedSelect}
            >
              <option value={0.5}>0.5x</option>
              <option value={1}>1x</option>
              <option value={2}>2x</option>
              <option value={4}>4x</option>
            </select>
          </div>
        </div>

        {/* 우측: 이벤트 패널 */}
        <div style={eventPanel}>
          <div style={panelHeader}>이벤트 타임라인</div>
          <div style={eventList}>
            {timeline.map((e) => (
              <div
                key={e.event_id}
                style={{
                  ...eventItem,
                  background: currentScreen?.event_id === e.event_id ? "#eef2ff" : "transparent",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                  <span
                    style={{
                      ...eventDot,
                      background: EVENT_COLORS[e.event_key] || "#6b7280",
                    }}
                  />
                  <span style={{ fontSize: 11, color: "#9ca3af", fontFamily: "monospace" }}>
                    {formatTime(e.occurred_at)}
                  </span>
                </div>
                <div style={{ fontSize: 12, fontWeight: 500, marginTop: 2 }}>{eventLabel(e)}</div>
              </div>
            ))}
          </div>

          {/* 디바이스 정보 */}
          <div style={deviceInfo}>
            {session.device && (
              <>
                <div>{session.device.device_model}</div>
                <div>
                  {session.device.os_name} {session.device.os_version}
                </div>
              </>
            )}
            <div style={{ marginTop: 4 }}>
              세션: {formatTime(session.started_at)} ~{" "}
              {session.ended_at ? formatTime(session.ended_at) : "진행 중"}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// 스타일
const backBtn: React.CSSProperties = {
  padding: "4px 12px",
  background: "#f3f4f6",
  border: "1px solid #e5e7eb",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 13,
};

const screenCard: React.CSSProperties = {
  flex: 1,
  background: "#fff",
  borderRadius: 8,
  boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
  display: "flex",
  flexDirection: "column",
  overflow: "hidden",
};

const screenHeader: React.CSSProperties = {
  padding: "10px 16px",
  borderBottom: "1px solid #f3f4f6",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
};

const screenLabel: React.CSSProperties = {
  fontWeight: 600,
  fontSize: 14,
  color: "#1e1b4b",
};

const durationLabel: React.CSSProperties = {
  fontSize: 12,
  color: "#6b7280",
  background: "#f3f4f6",
  padding: "2px 8px",
  borderRadius: 4,
};

const screenBody: React.CSSProperties = {
  flex: 1,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};

const screenPlaceholder: React.CSSProperties = {
  textAlign: "center",
  color: "#9ca3af",
};

const timelineBar: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  padding: "12px 0 4px",
};

const timelineDot: React.CSSProperties = {
  width: 12,
  height: 12,
  borderRadius: "50%",
  border: "none",
  cursor: "pointer",
  transition: "all 0.15s",
};

const screenNamesRow: React.CSSProperties = {
  display: "flex",
  padding: "0 8px 8px",
  gap: 4,
};

const controlBar: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  padding: "8px 0",
};

const controlBtn: React.CSSProperties = {
  width: 36,
  height: 36,
  borderRadius: 4,
  border: "1px solid #e5e7eb",
  background: "#f9fafb",
  cursor: "pointer",
  fontSize: 14,
};

const playBtn: React.CSSProperties = {
  width: 44,
  height: 44,
  borderRadius: "50%",
  border: "none",
  background: "#4f46e5",
  color: "#fff",
  cursor: "pointer",
  fontSize: 16,
};

const speedSelect: React.CSSProperties = {
  padding: "4px 8px",
  borderRadius: 4,
  border: "1px solid #d1d5db",
  fontSize: 13,
  marginLeft: 8,
};

const eventPanel: React.CSSProperties = {
  width: 280,
  background: "#fff",
  borderRadius: 8,
  boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
  display: "flex",
  flexDirection: "column",
  overflow: "hidden",
};

const panelHeader: React.CSSProperties = {
  padding: "10px 12px",
  fontWeight: 600,
  fontSize: 13,
  borderBottom: "1px solid #f3f4f6",
  color: "#1e1b4b",
};

const eventList: React.CSSProperties = {
  flex: 1,
  overflowY: "auto",
  padding: 8,
};

const eventItem: React.CSSProperties = {
  padding: "6px 8px",
  borderRadius: 4,
  marginBottom: 2,
};

const eventDot: React.CSSProperties = {
  width: 6,
  height: 6,
  borderRadius: "50%",
  flexShrink: 0,
};

const deviceInfo: React.CSSProperties = {
  padding: "10px 12px",
  borderTop: "1px solid #f3f4f6",
  fontSize: 11,
  color: "#6b7280",
};
