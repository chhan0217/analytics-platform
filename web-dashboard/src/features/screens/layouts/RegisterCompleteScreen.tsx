import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function RegisterCompleteScreen({ highlightId }: ScreenProps) {
  return (
    <div style={phone}>
      <StatusBar />
      <div style={appBar} />

      <div style={progressTrack}>
        <div style={{ ...progressFill, width: "100%" }} />
      </div>

      <div style={content}>
        <div style={checkCircle}>
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#4f46e5" strokeWidth="2.5">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>
        <div style={{ height: 40 }} />
        <div style={completeTitle}>회원가입 완료!</div>
      </div>

      <div style={bottomBar}>
        <Highlight elementId="startButton" activeId={highlightId} style={primaryBtn}>
          채비 시작하기
        </Highlight>
        <div style={{ height: 16 }} />
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
const appBar: React.CSSProperties = { height: 48, flexShrink: 0 };
const progressTrack: React.CSSProperties = { height: 4, background: "#f3f4f6", flexShrink: 0 };
const progressFill: React.CSSProperties = { height: "100%", background: "#4f46e5" };
const content: React.CSSProperties = { flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", padding: "0 20px" };
const checkCircle: React.CSSProperties = { width: 110, height: 110, borderRadius: "50%", background: "#EEF2FF", display: "flex", alignItems: "center", justifyContent: "center" };
const completeTitle: React.CSSProperties = { fontSize: 22, fontWeight: 800, color: "#111" };
const bottomBar: React.CSSProperties = { padding: "12px 20px 32px", flexShrink: 0 };
const primaryBtn: React.CSSProperties = { height: 48, borderRadius: 10, background: "#4f46e5", color: "#fff", fontSize: 14, fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "center" };
