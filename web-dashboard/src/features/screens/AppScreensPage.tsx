import { useState, useEffect } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse, ScreenLayoutEntry, ScreenLayoutDetail } from "../../api/types";
import { screenComponents } from "./layouts";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";

export default function AppScreensPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;

  const [layouts, setLayouts] = useState<ScreenLayoutEntry[]>([]);
  const [previews, setPreviews] = useState<ScreenLayoutDetail[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadLayouts = async () => {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api<DataResponse<ScreenLayoutEntry[]>>(
        `/admin/projects/${projectId}/screen-layouts`,
      );
      setLayouts(res.data);
    } catch {
      setError("화면 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const loadPreviews = async () => {
    if (!projectId || layouts.length === 0) return;
    try {
      const details = await Promise.all(
        layouts.map((l) =>
          api<DataResponse<ScreenLayoutDetail>>(
            `/admin/projects/${projectId}/screen-layouts/${l.id}`,
          ).then((res) => res.data),
        ),
      );
      setPreviews(details);
    } catch {
      /* ignore */
    }
  };

  useEffect(() => {
    loadLayouts();
  }, [projectId]);

  useEffect(() => {
    if (layouts.length > 0) loadPreviews();
  }, [layouts.length]);

  return (
    <div>
      <h2 style={titleStyle}>앱 화면</h2>

      {loading && <LoadingState />}
      {error && <ErrorState message={error} onRetry={loadLayouts} />}

      {!loading && !error && layouts.length === 0 && (
        <EmptyState icon="📱" title="등록된 화면이 없습니다" />
      )}

      {screenFlows.map((flow) => {
        const flowScreens = flow.screenIds
          .map((sid) => previews.find((p) => p.screen_id === sid))
          .filter(Boolean) as typeof previews;
        if (flowScreens.length === 0) return null;
        return (
          <div key={flow.label} style={{ marginBottom: 48 }}>
            <div style={flowHeader}>
              <span style={flowLabel}>{flow.label}</span>
              <span style={flowDesc}>{flow.desc}</span>
            </div>
            <div style={flowRow}>
              {flowScreens.map((p, i) => {
                const ScreenComp = screenComponents[p.screen_id];
                return (
                  <div key={p.id} style={{ display: "flex", alignItems: "center" }}>
                    <div style={previewCard}>
                      <div style={{ marginBottom: 8, textAlign: "center" }}>
                        <div style={flowStepBadge}>{i + 1}</div>
                        <div style={{ fontWeight: 600, fontSize: 13, color: "#1e1b4b" }}>
                          {p.display_name}
                        </div>
                        <div style={{ fontSize: 11, color: "#9ca3af", fontFamily: "monospace" }}>
                          {p.screen_id}
                        </div>
                      </div>
                      {ScreenComp ? <ScreenComp /> : <div style={previewEmpty}>화면 미등록</div>}
                    </div>
                    {i < flowScreens.length - 1 && (
                      <div style={flowArrow}>
                        <svg
                          width="24"
                          height="24"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="#c7d2fe"
                          strokeWidth="2"
                        >
                          <path d="M5 12h14M13 6l6 6-6 6" />
                        </svg>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}

const titleStyle: React.CSSProperties = {
  fontSize: 20,
  fontWeight: 600,
  color: "#1e1b4b",
  marginBottom: 16,
};

const screenFlows = [
  {
    label: "온보딩",
    desc: "앱 실행 → 스플래시 → 로그인/회원가입 선택",
    screenIds: ["splash", "onboard"],
  },
  {
    label: "로그인",
    desc: "이메일 로그인 → 아이디 찾기 / 비밀번호 재설정",
    screenIds: ["login", "find_id", "reset_password"],
  },
  {
    label: "회원가입",
    desc: "약관동의 → 본인인증 → 정보입력 → 완료",
    screenIds: ["register_term", "register_pass", "register_input", "register_complete"],
  },
];

const flowHeader: React.CSSProperties = {
  marginBottom: 16,
  paddingBottom: 8,
  borderBottom: "2px solid #e5e7eb",
};

const flowLabel: React.CSSProperties = {
  fontSize: 16,
  fontWeight: 700,
  color: "#1e1b4b",
  marginRight: 12,
};

const flowDesc: React.CSSProperties = {
  fontSize: 13,
  color: "#9ca3af",
};

const flowRow: React.CSSProperties = {
  display: "flex",
  gap: 8,
  overflowX: "auto",
  paddingBottom: 8,
};

const flowArrow: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  padding: "0 4px",
  flexShrink: 0,
};

const flowStepBadge: React.CSSProperties = {
  width: 22,
  height: 22,
  borderRadius: "50%",
  background: "#4f46e5",
  color: "#fff",
  fontSize: 11,
  fontWeight: 700,
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  marginBottom: 4,
};

const previewCard: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
};

const previewEmpty: React.CSSProperties = {
  width: 240,
  height: 480,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f9fafb",
  borderRadius: 24,
  border: "1px dashed #d1d5db",
  color: "#9ca3af",
};
