interface LayoutNode {
  id?: string;
  type: string;
  value?: string;
  width?: string | number;
  height?: number;
  children?: LayoutNode[];
  items?: string[];
  active?: string;
  alignment?: string;
  underline?: boolean;
  distribution?: string;
  spacing?: number;
  padding?: { horizontal?: number; vertical?: number };
}

interface ScreenLayout {
  root?: LayoutNode;
  [key: string]: unknown;
}

interface Props {
  layout: ScreenLayout;
  maxHeight?: number;
  fitContainer?: boolean;
}

export default function LayoutWireframe({ layout, maxHeight = 700, fitContainer }: Props) {
  if (!layout.root) {
    return (
      <div style={phone(280, maxHeight)}>
        <div style={emptyMsg}>지원하지 않는 포맷</div>
      </div>
    );
  }

  // fitContainer: 부모 크기에 맞춰 비율 유지 스케일링
  if (fitContainer) {
    return (
      <div style={{ width: "100%", display: "flex", justifyContent: "center" }}>
        <div style={{ width: "100%", maxWidth: 320, aspectRatio: "9 / 19.5" }}>
          <div
            style={{
              width: "100%",
              height: "100%",
              ...phoneBase,
              overflow: "hidden",
            }}
          >
            <StatusBar />
            <div style={contentArea}>
              <NodeRenderer node={layout.root} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // root.children에서 header, navigation 분리
  const children = layout.root.children || [];
  const headerNode = children.find((c) => c.type === "header");
  const navNode = children.find((c) => c.type === "navigation");
  const bodyNodes = children.filter((c) => c.type !== "header" && c.type !== "navigation");

  return (
    <div style={phone(280, maxHeight)}>
      <StatusBar />
      {headerNode && <NodeRenderer node={headerNode} />}
      <div style={contentArea}>
        {bodyNodes.map((child, i) => (
          <NodeRenderer key={child.id || i} node={child} />
        ))}
      </div>
      {navNode && <NodeRenderer node={navNode} />}
    </div>
  );
}

function StatusBar() {
  return (
    <div style={statusBarStyle}>
      <span style={{ fontWeight: 600 }}>9:41</span>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="#111">
          <rect x="0" y="6" width="2.5" height="4" rx="0.5" />
          <rect x="3.5" y="4" width="2.5" height="6" rx="0.5" />
          <rect x="7" y="2" width="2.5" height="8" rx="0.5" />
          <rect x="10.5" y="0" width="2.5" height="10" rx="0.5" />
        </svg>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="none" stroke="#111" strokeWidth="1.2">
          <path d="M7 2C9.5 2 11.5 3.5 13 5.5M7 2C4.5 2 2.5 3.5 1 5.5M7 5C8.3 5 9.5 5.7 10.3 6.8M7 5C5.7 5 4.5 5.7 3.7 6.8" />
          <circle cx="7" cy="8.5" r="1" fill="#111" stroke="none" />
        </svg>
        <svg width="22" height="11" viewBox="0 0 22 11" fill="none">
          <rect x="0.5" y="0.5" width="19" height="10" rx="2" stroke="#111" strokeWidth="1" />
          <rect x="2" y="2" width="14" height="7" rx="1" fill="#34D399" />
          <rect x="20" y="3.5" width="2" height="4" rx="0.5" fill="#111" />
        </svg>
      </div>
    </div>
  );
}

function NodeRenderer({ node }: { node: LayoutNode }) {
  const paddingH = node.padding?.horizontal || 0;
  const paddingV = node.padding?.vertical || 0;
  const gap = node.spacing || 0;

  switch (node.type) {
    case "column":
      return (
        <div
          style={{
            display: "flex",
            flexDirection: "column",
            width: node.width || "100%",
            flex: node.height ? undefined : 1,
            height: node.height,
            gap,
            padding: paddingH || paddingV ? `${paddingV}px ${paddingH}px` : undefined,
            boxSizing: "border-box",
          }}
        >
          {node.children?.map((child, i) => (
            <NodeRenderer key={child.id || i} node={child} />
          ))}
        </div>
      );

    case "row":
      return (
        <div
          style={{
            display: "flex",
            gap: gap || 8,
            padding: paddingH || paddingV ? `${paddingV}px ${paddingH}px` : "8px 16px",
            alignItems: "center",
            height: node.height,
            justifyContent: node.distribution === "spaceEvenly" ? "space-evenly" : undefined,
            boxSizing: "border-box",
          }}
        >
          {node.children?.map((child, i) => (
            <NodeRenderer key={child.id || i} node={child} />
          ))}
        </div>
      );

    case "header":
      return (
        <div style={{ ...headerStyle, height: node.height || 48 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, width: "100%" }}>
            <svg
              width="20"
              height="20"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#333"
              strokeWidth="2"
            >
              <path d="M15 18l-6-6 6-6" />
            </svg>
            <span
              style={{
                flex: 1,
                textAlign: "center",
                fontWeight: 600,
                fontSize: 15,
                color: "#111",
                marginRight: 20,
              }}
            >
              {node.children?.find((c) => c.type === "text")?.value || node.value || ""}
            </span>
          </div>
        </div>
      );

    case "spacer":
      return <div style={{ height: node.height || 16, flexShrink: 0 }} />;

    case "divider":
      return (
        <div
          style={{
            flex: 1,
            width: node.width,
            height: 1,
            background: "#e0e0e0",
            alignSelf: "center",
          }}
        />
      );

    case "text":
      return (
        <div
          style={{
            fontSize: 13,
            color: "#333",
            padding: "6px 16px",
            lineHeight: 1.5,
            textAlign: node.alignment === "center" ? "center" : undefined,
            textDecoration: node.underline ? "underline" : undefined,
            height: node.height,
            display: "flex",
            alignItems: "center",
            justifyContent: node.alignment === "center" ? "center" : undefined,
          }}
        >
          {node.value}
        </div>
      );

    case "button":
      return (
        <div
          style={{
            ...btnStyle,
            width: node.width || undefined,
            height: node.height || undefined,
            margin: node.width === "100%" ? "4px 0" : "4px 16px",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
          }}
        >
          <span>{node.value}</span>
        </div>
      );

    case "card":
      return (
        <div style={{ ...cardStyle, width: node.width || "100%" }}>
          {node.children?.map((child, i) => (
            <NodeRenderer key={child.id || i} node={child} />
          ))}
          {!node.children && node.value && (
            <>
              <div style={{ fontSize: 11, color: "#888", marginBottom: 2 }}>{node.value}</div>
              <div style={{ fontSize: 20, fontWeight: 700, color: "#111" }}>—</div>
            </>
          )}
        </div>
      );

    case "imagePlaceholder":
      return (
        <div
          style={{
            ...imgPlaceholder,
            width: node.width || undefined,
            height: node.height || 100,
            margin: node.alignment === "center" ? "8px auto" : "8px 16px",
            borderRadius: node.width === node.height ? "50%" : 12,
          }}
        >
          <svg
            width="24"
            height="24"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#bbb"
            strokeWidth="1.2"
          >
            <rect x="3" y="3" width="18" height="18" rx="3" />
            <circle cx="8.5" cy="8.5" r="1.5" />
            <path d="M21 15l-5-5L5 21" />
          </svg>
          {node.value && (
            <span
              style={{
                fontSize: Math.min(10, (node.width as number) ? (node.width as number) / 8 : 10),
                color: "#aaa",
                marginTop: 4,
                textAlign: "center",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
                maxWidth: "100%",
                padding: "0 2px",
              }}
            >
              {node.value}
            </span>
          )}
        </div>
      );

    case "list":
      return (
        <div style={{ flex: 1 }}>
          {node.items?.map((item, i) => (
            <div key={i} style={listItem}>
              <span style={{ fontSize: 13, color: "#333" }}>{item}</span>
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#ccc"
                strokeWidth="2"
              >
                <path d="M9 18l6-6-6-6" />
              </svg>
            </div>
          ))}
        </div>
      );

    case "navigation": {
      const activeItem = node.active || node.items?.[node.items.length > 3 ? 3 : 0];
      return (
        <div style={navBar}>
          {node.items?.map((item, i) => {
            const isActive = item === activeItem;
            return (
              <div key={i} style={navItem}>
                <div
                  style={{
                    width: 22,
                    height: 22,
                    borderRadius: 6,
                    background: isActive ? "#EEF2FF" : "transparent",
                    border: `1.5px solid ${isActive ? "#4f46e5" : "#d1d5db"}`,
                  }}
                />
                <span
                  style={{
                    fontSize: 9,
                    marginTop: 2,
                    color: isActive ? "#4f46e5" : "#9ca3af",
                    fontWeight: isActive ? 600 : 400,
                  }}
                >
                  {item}
                </span>
              </div>
            );
          })}
        </div>
      );
    }

    default:
      return null;
  }
}

/* 스타일 */
const phoneBase: React.CSSProperties = {
  background: "#fff",
  borderRadius: 32,
  display: "flex",
  flexDirection: "column",
  boxShadow: "0 2px 40px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.06)",
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const phone = (w: number, h: number): React.CSSProperties => ({
  width: w,
  height: h,
  overflow: "hidden",
  margin: "0 auto",
  ...phoneBase,
});

const emptyMsg: React.CSSProperties = {
  flex: 1,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  color: "#9ca3af",
  fontSize: 13,
};

const statusBarStyle: React.CSSProperties = {
  height: 44,
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-end",
  padding: "0 20px 6px",
  fontSize: 13,
  color: "#111",
  flexShrink: 0,
};

const contentArea: React.CSSProperties = {
  flex: 1,
  overflow: "auto",
  display: "flex",
  flexDirection: "column",
};

const headerStyle: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  padding: "0 12px",
  flexShrink: 0,
  borderBottom: "1px solid #f0f0f0",
};

const btnStyle: React.CSSProperties = {
  margin: "4px 16px",
  padding: "12px 0",
  background: "#4f46e5",
  color: "#fff",
  borderRadius: 10,
  fontSize: 13,
  fontWeight: 600,
  textAlign: "center",
  flexShrink: 0,
  boxSizing: "border-box",
};

const cardStyle: React.CSSProperties = {
  background: "#f8f9fb",
  borderRadius: 12,
  padding: 14,
  margin: "4px 0",
  display: "flex",
  flexDirection: "column",
  gap: 6,
  border: "1px solid #eee",
  textAlign: "center",
  boxSizing: "border-box",
};

const imgPlaceholder: React.CSSProperties = {
  background: "#f5f5f7",
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  border: "1px dashed #ddd",
  boxSizing: "border-box",
  flexShrink: 0,
};

const listItem: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  padding: "14px 16px",
  borderBottom: "1px solid #f3f3f3",
};

const navBar: React.CSSProperties = {
  height: 60,
  background: "#fff",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-around",
  borderTop: "1px solid #eee",
  flexShrink: 0,
  marginTop: "auto",
  paddingBottom: 4,
};

const navItem: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  gap: 1,
};
