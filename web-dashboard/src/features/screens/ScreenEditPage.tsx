import { useState, useEffect, useRef, Component, type ReactNode } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse, ScreenLayoutDetail } from "../../api/types";
import LayoutWireframe from "./components/LayoutWireframe";

class PreviewErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidUpdate(prevProps: { children: ReactNode }) {
    if (prevProps.children !== this.props.children) {
      this.setState({ hasError: false });
    }
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          style={{
            width: 320,
            height: 640,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            background: "#fef2f2",
            borderRadius: 24,
            border: "1px solid #fecaca",
          }}
        >
          <span style={{ color: "#ef4444", fontSize: 13 }}>지원하지 않는 JSON 포맷</span>
        </div>
      );
    }
    return this.props.children;
  }
}

export default function ScreenEditPage() {
  const { layoutId } = useParams<{ layoutId: string }>();
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const navigate = useNavigate();
  const fileRef = useRef<HTMLInputElement>(null);

  const [layout, setLayout] = useState<ScreenLayoutDetail | null>(null);
  const [jsonText, setJsonText] = useState("");
  const [parsedLayout, setParsedLayout] = useState<object | null>(null);
  const [jsonError, setJsonError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!projectId || !layoutId) return;
    setLoading(true);
    api<DataResponse<ScreenLayoutDetail>>(`/admin/projects/${projectId}/screen-layouts/${layoutId}`)
      .then((res) => {
        setLayout(res.data);
        setJsonText(res.data.layout_json);
      })
      .catch(() => navigate("/app-screens"))
      .finally(() => setLoading(false));
  }, [projectId, layoutId]);

  useEffect(() => {
    try {
      const parsed = JSON.parse(jsonText);
      setParsedLayout(parsed);
      setJsonError(null);
    } catch (e) {
      setParsedLayout(null);
      setJsonError((e as Error).message);
    }
    setSaved(false);
  }, [jsonText]);

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setJsonText(reader.result as string);
    reader.readAsText(file);
    if (fileRef.current) fileRef.current.value = "";
  };

  const handleSave = async () => {
    if (!projectId || !layoutId || jsonError) return;
    setSaving(true);
    try {
      await api(`/admin/projects/${projectId}/screen-layouts/${layoutId}`, {
        method: "PATCH",
        body: JSON.stringify({ layout_json: jsonText }),
      });
      setSaved(true);
    } catch {
      /* ignore */
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p style={{ color: "#9ca3af" }}>로딩 중...</p>;
  if (!layout) return null;

  return (
    <div
      style={{
        height: "calc(100vh - 120px)",
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
      }}
    >
      {/* 헤더 */}
      <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16 }}>
        <button onClick={() => navigate("/app-screens")} style={backBtn}>
          &larr; 목록
        </button>
        <div>
          <h2 style={{ margin: 0, fontSize: 18, fontWeight: 600, color: "#1e1b4b" }}>
            {layout.display_name}
          </h2>
          <span style={{ fontSize: 12, color: "#6b7280", fontFamily: "monospace" }}>
            screen_id: {layout.screen_id}
          </span>
        </div>
        <div style={{ marginLeft: "auto", display: "flex", gap: 8, alignItems: "center" }}>
          <input
            ref={fileRef}
            type="file"
            accept=".json"
            onChange={handleFileUpload}
            style={{ display: "none" }}
          />
          <button onClick={() => fileRef.current?.click()} style={fileBtn}>
            JSON 파일 불러오기
          </button>
          {saved && <span style={{ fontSize: 12, color: "#10b981" }}>저장 완료</span>}
          <button
            onClick={handleSave}
            disabled={saving || !!jsonError}
            style={{
              ...saveBtn,
              opacity: jsonError ? 0.5 : 1,
            }}
          >
            {saving ? "저장 중..." : "저장"}
          </button>
        </div>
      </div>

      {/* 에디터 + 미리보기 */}
      <div style={{ display: "flex", gap: 20, height: "calc(100vh - 180px)", overflow: "hidden" }}>
        {/* 좌측: JSON 에디터 */}
        <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
          <div style={{ fontSize: 12, fontWeight: 600, color: "#374151", marginBottom: 6 }}>
            레이아웃 JSON
          </div>
          <textarea
            value={jsonText}
            onChange={(e) => setJsonText(e.target.value)}
            style={editorStyle}
            spellCheck={false}
          />
          {jsonError && (
            <div style={{ color: "#ef4444", fontSize: 11, marginTop: 4 }}>
              JSON 오류: {jsonError}
            </div>
          )}
          <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 6 }}>
            타입: column, row, header, text, button, card, list, navigation, imagePlaceholder
          </div>
        </div>

        {/* 우측: 미리보기 (고정 크기) */}
        <div
          style={{
            width: 340,
            flexShrink: 0,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
          }}
        >
          <div style={{ fontSize: 12, fontWeight: 600, color: "#374151", marginBottom: 6 }}>
            미리보기
          </div>
          <div style={{ width: 320, height: 640, overflow: "hidden" }}>
            {parsedLayout ? (
              <PreviewErrorBoundary key={jsonText}>
                <LayoutWireframe layout={parsedLayout as never} maxHeight={640} />
              </PreviewErrorBoundary>
            ) : (
              <div style={emptyPreview}>
                <span style={{ color: "#9ca3af" }}>올바른 JSON을 입력하세요</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

const fileBtn: React.CSSProperties = {
  padding: "8px 16px",
  background: "#f3f4f6",
  color: "#374151",
  border: "1px solid #e5e7eb",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
};

const backBtn: React.CSSProperties = {
  padding: "4px 12px",
  background: "#f3f4f6",
  border: "1px solid #e5e7eb",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 13,
};

const saveBtn: React.CSSProperties = {
  padding: "8px 20px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
  fontWeight: 500,
};

const editorStyle: React.CSSProperties = {
  flex: 1,
  fontFamily: "monospace",
  fontSize: 12,
  padding: 12,
  borderRadius: 8,
  border: "1px solid #d1d5db",
  background: "#1e1b4b",
  color: "#e0e7ff",
  resize: "none",
  lineHeight: 1.6,
};

const emptyPreview: React.CSSProperties = {
  width: 320,
  height: 640,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  background: "#f9fafb",
  borderRadius: 24,
  border: "1px dashed #d1d5db",
};
