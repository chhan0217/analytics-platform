import type { TemplateSnapshot, WireframeNode } from "../../../api/types";

interface Props {
  snapshot: TemplateSnapshot;
  maxHeight: number;
}

export default function WireframeRenderer({ snapshot, maxHeight }: Props) {
  const { viewport, wireframe } = snapshot;
  const scale = maxHeight / viewport.height;
  const scaledWidth = viewport.width * scale;

  return (
    <div
      style={{
        width: scaledWidth,
        height: maxHeight,
        position: "relative",
        background: "#121212",
        borderRadius: 12,
        overflow: "hidden",
        margin: "0 auto",
        boxShadow: "0 4px 24px rgba(0,0,0,0.15)",
      }}
    >
      {wireframe.map((node) => (
        <NodeRenderer key={node.id} node={node} scale={scale} />
      ))}
    </div>
  );
}

function NodeRenderer({ node, scale }: { node: WireframeNode; scale: number }) {
  const s = node.style;
  const bgColor = s?.background_color;
  const textColor = s?.text_color;
  const fontSizePx = s?.font_size ? s.font_size * scale : undefined;
  const borderRadius = s?.corner_radius ? Math.min(s.corner_radius * scale, 9999) : undefined;

  const baseStyle: React.CSSProperties = {
    position: "absolute",
    left: node.x * scale,
    top: node.y * scale,
    width: node.width * scale,
    height: node.height * scale,
    boxSizing: "border-box",
    display: "flex",
    alignItems: "center",
    overflow: "hidden",
    transition: "opacity 0.2s",
  };

  // 타입별 기본 스타일 + SDK에서 온 스타일 오버라이드
  const typeStyle = getTypeStyle(node.type, scale);
  const sdkStyle: React.CSSProperties = {};
  if (bgColor) sdkStyle.background = bgColor;
  if (textColor) sdkStyle.color = textColor;
  if (borderRadius !== undefined) sdkStyle.borderRadius = borderRadius;
  if (s?.alpha !== undefined && s.alpha < 1) sdkStyle.opacity = s.alpha;

  const finalStyle = { ...baseStyle, ...typeStyle, ...sdkStyle };

  const fontSize = fontSizePx || Math.max(7, Math.min(13, node.height * scale * 0.4));

  return (
    <div style={finalStyle}>
      {node.text && (
        <span
          style={{
            fontSize,
            lineHeight: 1.2,
            padding: "0 3px",
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
            width: "100%",
            textAlign: node.type === "Button" || node.type === "Tab" ? "center" : "left",
            fontWeight: node.type === "Button" ? 600 : undefined,
            letterSpacing: node.type === "Button" ? 0.5 : undefined,
          }}
        >
          {node.text}
        </span>
      )}
      {node.type === "Image" && (
        <div
          style={{
            width: "100%",
            height: "100%",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            flexDirection: "column",
            gap: 2,
          }}
        >
          <svg
            width={Math.min(20, node.width * scale * 0.3)}
            height={Math.min(20, node.height * scale * 0.3)}
            viewBox="0 0 24 24"
            fill="none"
            stroke="#aaa"
            strokeWidth="1.5"
          >
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
          {s?.content_description && (
            <span style={{ fontSize: 7, color: "#999", textAlign: "center" }}>
              {s.content_description}
            </span>
          )}
        </div>
      )}
      {node.type === "Checkbox" && (
        <div
          style={{
            width: "60%",
            height: "60%",
            margin: "auto",
            border: "1.5px solid #888",
            borderRadius: 3,
            background: "#fff",
          }}
        />
      )}
      {node.type === "Switch" && (
        <div
          style={{
            width: "100%",
            height: "100%",
            borderRadius: 999,
            background: bgColor || "#C8E6C9",
            position: "relative",
          }}
        >
          <div
            style={{
              position: "absolute",
              right: 2,
              top: "50%",
              transform: "translateY(-50%)",
              width: node.height * scale * 0.7,
              height: node.height * scale * 0.7,
              borderRadius: "50%",
              background: "#fff",
              boxShadow: "0 1px 2px rgba(0,0,0,0.2)",
            }}
          />
        </div>
      )}
      {node.children?.map((child) => (
        <NodeRenderer key={child.id} node={child} scale={scale} />
      ))}
    </div>
  );
}

function getTypeStyle(type: string, scale: number): React.CSSProperties {
  switch (type) {
    case "StatusBar":
      return { background: "#1a1a2e", zIndex: 10 };
    case "NavigationBar":
      return { background: "#1a1a2e", zIndex: 10 };
    case "Button":
      return {
        background: "#3b82f6",
        borderRadius: 8 * scale,
        justifyContent: "center",
        color: "#fff",
        boxShadow: "0 1px 3px rgba(0,0,0,0.12)",
      };
    case "Text":
      return {
        color: "#e0e0e0",
      };
    case "Image":
      return {
        background: "#2a2a3e",
        borderRadius: 4 * scale,
      };
    case "TextField":
      return {
        background: "#ffffff10",
        border: "1px solid #555",
        borderRadius: 8 * scale,
        color: "#888",
        padding: "0 4px",
      };
    case "List":
      return {
        borderRadius: 8 * scale,
      };
    case "ListItem":
      return {
        borderBottom: "1px solid #333",
        color: "#ccc",
        padding: "0 4px",
      };
    case "Checkbox":
      return {};
    case "Switch":
      return {};
    case "Tab":
      return {
        borderBottom: "2px solid #3b82f6",
        justifyContent: "center",
        color: "#3b82f6",
      };
    default:
      return {};
  }
}
