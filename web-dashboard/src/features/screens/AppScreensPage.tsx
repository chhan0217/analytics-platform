import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse, ScreenLayoutEntry, ScreenLayoutDetail } from "../../api/types";
import LayoutWireframe from "./components/LayoutWireframe";

export default function AppScreensPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const navigate = useNavigate();

  const [layouts, setLayouts] = useState<ScreenLayoutEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [newScreenId, setNewScreenId] = useState("");
  const [newDisplayName, setNewDisplayName] = useState("");
  const [creating, setCreating] = useState(false);
  const [tab, setTab] = useState<"list" | "preview">("list");
  const [previews, setPreviews] = useState<ScreenLayoutDetail[]>([]);
  const [loadingPreviews, setLoadingPreviews] = useState(false);

  const loadLayouts = async () => {
    if (!projectId) return;
    setLoading(true);
    try {
      const res = await api<DataResponse<ScreenLayoutEntry[]>>(
        `/admin/projects/${projectId}/screen-layouts`,
      );
      setLayouts(res.data);
    } catch {
      /* ignore */
    } finally {
      setLoading(false);
    }
  };

  const loadPreviews = async () => {
    if (!projectId || layouts.length === 0) return;
    setLoadingPreviews(true);
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
    } finally {
      setLoadingPreviews(false);
    }
  };

  useEffect(() => {
    loadLayouts();
  }, [projectId]);

  useEffect(() => {
    if (tab === "preview") loadPreviews();
  }, [tab, layouts.length]);

  const handleCreate = async () => {
    if (!projectId || !newScreenId.trim() || !newDisplayName.trim()) return;
    setCreating(true);
    try {
      const defaultJson = JSON.stringify(
        {
          screenId: newScreenId.trim(),
          version: 1,
          root: {
            type: "column",
            children: [
              { type: "header", children: [{ type: "text", value: newDisplayName.trim() }] },
            ],
          },
        },
        null,
        2,
      );

      const res = await api<DataResponse<{ id: string }>>(
        `/admin/projects/${projectId}/screen-layouts`,
        {
          method: "POST",
          body: JSON.stringify({
            screen_id: newScreenId.trim(),
            display_name: newDisplayName.trim(),
            layout_json: defaultJson,
          }),
        },
      );

      setNewScreenId("");
      setNewDisplayName("");
      setShowCreate(false);
      navigate(`/app-screens/${res.data.id}`);
    } catch {
      /* ignore */
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (id: string, screenId: string) => {
    if (!confirm(`"${screenId}" 화면을 삭제하시겠습니까?`)) return;
    try {
      await api(`/admin/projects/${projectId}/screen-layouts/${id}`, { method: "DELETE" });
      await loadLayouts();
    } catch {
      /* ignore */
    }
  };

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 12,
        }}
      >
        <h2 style={titleStyle}>앱 화면</h2>
        <button onClick={() => setShowCreate(true)} style={createBtn}>
          + 새 화면 추가
        </button>
      </div>

      {/* 탭 */}
      <div style={{ display: "flex", gap: 4, marginBottom: 16 }}>
        <button onClick={() => setTab("list")} style={tabBtn(tab === "list")}>
          목록
        </button>
        <button onClick={() => setTab("preview")} style={tabBtn(tab === "preview")}>
          미리보기
        </button>
      </div>

      {showCreate && (
        <div style={createCard}>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <input
              value={newScreenId}
              onChange={(e) => setNewScreenId(e.target.value)}
              placeholder="screen_id (예: login, mypage)"
              style={inputStyle}
            />
            <input
              value={newDisplayName}
              onChange={(e) => setNewDisplayName(e.target.value)}
              placeholder="표시 이름 (예: 로그인, 마이페이지)"
              style={inputStyle}
            />
            <button onClick={handleCreate} disabled={creating} style={primaryBtn}>
              {creating ? "생성 중..." : "생성"}
            </button>
            <button onClick={() => setShowCreate(false)} style={cancelBtn}>
              취소
            </button>
          </div>
        </div>
      )}

      {loading && <p style={{ color: "#9ca3af" }}>로딩 중...</p>}

      {layouts.length === 0 && !loading && (
        <div style={emptyState}>
          <div style={{ fontSize: 40, marginBottom: 12, opacity: 0.3 }}>📱</div>
          <div style={{ fontSize: 15, color: "#6b7280" }}>등록된 화면이 없습니다</div>
          <div style={{ fontSize: 13, color: "#9ca3af", marginTop: 4 }}>
            "새 화면 추가" 버튼을 눌러 앱 화면을 만들어보세요
          </div>
        </div>
      )}

      {tab === "list" ? (
        /* 목록 탭 */
        <div style={gridStyle}>
          {layouts.map((layout) => (
            <div key={layout.id} style={cardStyle}>
              <div
                style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}
              >
                <div>
                  <div style={{ fontWeight: 600, fontSize: 15, color: "#1e1b4b" }}>
                    {layout.display_name}
                  </div>
                  <div
                    style={{
                      fontSize: 12,
                      color: "#6b7280",
                      marginTop: 2,
                      fontFamily: "monospace",
                    }}
                  >
                    {layout.screen_id}
                  </div>
                </div>
                <button onClick={() => handleDelete(layout.id, layout.screen_id)} style={deleteBtn}>
                  삭제
                </button>
              </div>
              <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 8 }}>
                수정:{" "}
                {new Date(layout.updated_at).toLocaleString("ko-KR", { timeZone: "Asia/Seoul" })}
              </div>
              <button onClick={() => navigate(`/app-screens/${layout.id}`)} style={editBtn}>
                편집
              </button>
            </div>
          ))}
        </div>
      ) : (
        /* 미리보기 탭 */
        <div>
          {loadingPreviews && <p style={{ color: "#9ca3af" }}>미리보기 로딩 중...</p>}
          <div style={previewGrid}>
            {previews.map((p) => {
              let parsed = null;
              try {
                parsed = JSON.parse(p.layout_json);
              } catch {
                /* ignore */
              }
              return (
                <div key={p.id} style={previewCard}>
                  <div style={{ marginBottom: 8, textAlign: "center" }}>
                    <div style={{ fontWeight: 600, fontSize: 13, color: "#1e1b4b" }}>
                      {p.display_name}
                    </div>
                    <div style={{ fontSize: 11, color: "#9ca3af", fontFamily: "monospace" }}>
                      {p.screen_id}
                    </div>
                  </div>
                  {parsed ? (
                    <LayoutWireframe layout={parsed} maxHeight={480} />
                  ) : (
                    <div style={previewEmpty}>JSON 오류</div>
                  )}
                  <button
                    onClick={() => navigate(`/app-screens/${p.id}`)}
                    style={{ ...editBtn, marginTop: 8 }}
                  >
                    편집
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

const titleStyle: React.CSSProperties = {
  fontSize: 20,
  fontWeight: 600,
  color: "#1e1b4b",
  margin: 0,
};

const tabBtn = (active: boolean): React.CSSProperties => ({
  padding: "6px 14px",
  background: active ? "#4f46e5" : "#f3f4f6",
  color: active ? "#fff" : "#374151",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 500,
});

const createBtn: React.CSSProperties = {
  padding: "8px 16px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 500,
};

const createCard: React.CSSProperties = {
  padding: 16,
  background: "#fff",
  borderRadius: 8,
  border: "1px solid #e5e7eb",
  marginBottom: 16,
};

const inputStyle: React.CSSProperties = {
  padding: "8px 12px",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  fontSize: 13,
  flex: 1,
};

const primaryBtn: React.CSSProperties = {
  padding: "8px 16px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
};

const cancelBtn: React.CSSProperties = {
  padding: "8px 16px",
  background: "#f3f4f6",
  color: "#374151",
  border: "1px solid #e5e7eb",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
};

const emptyState: React.CSSProperties = { textAlign: "center", padding: "60px 0" };

const gridStyle: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
  gap: 12,
};

const cardStyle: React.CSSProperties = {
  padding: 16,
  background: "#fff",
  borderRadius: 8,
  border: "1px solid #e5e7eb",
  boxShadow: "0 1px 2px rgba(0,0,0,0.04)",
};

const editBtn: React.CSSProperties = {
  marginTop: 12,
  width: "100%",
  padding: "8px 0",
  background: "#f3f4f6",
  color: "#374151",
  border: "1px solid #e5e7eb",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 500,
};

const deleteBtn: React.CSSProperties = {
  padding: "2px 8px",
  background: "none",
  color: "#ef4444",
  border: "1px solid #fecaca",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 11,
};

const previewGrid: React.CSSProperties = {
  display: "flex",
  gap: 20,
  flexWrap: "wrap",
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
