import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function RegisterPassScreen({ highlightId }: ScreenProps) {
  return (
    <div style={phone}>
      <StatusBar />
      <div style={appBar}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#333" strokeWidth="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </div>

      <div style={progressTrack}>
        <div style={{ ...progressFill, width: "50%" }} />
      </div>

      <div style={content}>
        <div style={{ height: 40 }} />
        <div style={title}>본인 인증</div>
        <div style={{ height: 40 }} />
        <div style={desc}>본인 인증 수단을 선택해주세요.</div>
        <div style={{ height: 24 }} />
        <div style={divider} />
        <div style={{ height: 24 }} />

        <Highlight elementId="phoneVerifyButton" activeId={highlightId} style={outlinedBtn}>
          휴대폰 본인 인증하기
        </Highlight>
      </div>
    </div>
  );
}

function StatusBar() {
  return (
    <div style={statusBar}>
      <span style={{ fontWeight: 600 }}>9:41</span>
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <svg width="14" height="10" viewBox="0 0 14 10" fill="#111">
          <rect x="0" y="6" width="2.5" height="4" rx="0.5" />
          <rect x="3.5" y="4" width="2.5" height="6" rx="0.5" />
          <rect x="7" y="2" width="2.5" height="8" rx="0.5" />
          <rect x="10.5" y="0" width="2.5" height="10" rx="0.5" />
        </svg>
        <svg width="22" height="11" viewBox="0 0 22 11" fill="none">
          <rect x="0.5" y="0.5" width="19" height="10" rx="2" stroke="#111" />
          <rect x="2" y="2" width="14" height="7" rx="1" fill="#34D399" />
          <rect x="20" y="3.5" width="2" height="4" rx="0.5" fill="#111" />
        </svg>
      </div>
    </div>
  );
}

const phone: React.CSSProperties = { width: 280, height: 700, background: "#fff", borderRadius: 32, display: "flex", flexDirection: "column", boxShadow: "0 2px 40px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.06)", fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif", overflow: "hidden", margin: "0 auto" };
const statusBar: React.CSSProperties = { height: 44, display: "flex", justifyContent: "space-between", alignItems: "flex-end", padding: "0 20px 6px", fontSize: 13, color: "#111", flexShrink: 0 };
const appBar: React.CSSProperties = { height: 48, display: "flex", alignItems: "center", padding: "0 12px", flexShrink: 0, borderBottom: "1px solid #f0f0f0" };
const progressTrack: React.CSSProperties = { height: 4, background: "#f3f4f6", flexShrink: 0 };
const progressFill: React.CSSProperties = { height: "100%", background: "#4f46e5" };
const content: React.CSSProperties = { flex: 1, display: "flex", flexDirection: "column", padding: "0 20px", overflow: "auto" };
const title: React.CSSProperties = { fontSize: 22, fontWeight: 800, color: "#111" };
const desc: React.CSSProperties = { fontSize: 13, color: "#333" };
const divider: React.CSSProperties = { height: 1, background: "#e5e7eb" };
const outlinedBtn: React.CSSProperties = { height: 48, borderRadius: 8, border: "1px solid #4f46e5", background: "#fff", color: "#4f46e5", fontSize: 14, fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 };
