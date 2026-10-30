import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function OnboardingScreen({ highlightId }: ScreenProps) {
  return (
    <div style={phone}>
      <StatusBar />
      <div style={content}>
        {/* 로고 영역 (중앙) */}
        <div style={logoArea}>
          <div style={logoPlaceholder}>
            <svg width="120" height="28" viewBox="0 0 120 28" fill="none">
              <rect width="120" height="28" rx="4" fill="#f0f0f0" />
              <text x="60" y="18" textAnchor="middle" fontSize="12" fill="#999">
                CHAEVI
              </text>
            </svg>
          </div>
        </div>

        <Highlight elementId="emailLoginButton" activeId={highlightId} style={outlinedBtn}>
          이메일 로그인
        </Highlight>

        <div style={{ height: 12 }} />

        <Highlight elementId="emailRegisterButton" activeId={highlightId} style={primaryBtn}>
          이메일 회원가입
        </Highlight>

        <div style={{ height: 16 }} />

        <Highlight elementId="skipToMain" activeId={highlightId} style={skipText}>
          채비 앱 둘러보기
        </Highlight>

        <div style={{ height: 72 }} />

        <div style={dividerRow}>
          <div style={dividerLine} />
          <span style={dividerLabel}>간편로그인</span>
          <div style={dividerLine} />
        </div>

        <div style={{ height: 24 }} />

        <div style={socialRow}>
          <Highlight elementId="kakaoLogin" activeId={highlightId}>
            <SocialIcon color="#FEE500" label="K" />
          </Highlight>
          <Highlight elementId="naverLogin" activeId={highlightId}>
            <SocialIcon color="#03C75A" label="N" textColor="#fff" />
          </Highlight>
          <Highlight elementId="googleLogin" activeId={highlightId}>
            <SocialIcon color="#fff" label="G" border />
          </Highlight>
          <Highlight elementId="appleLogin" activeId={highlightId}>
            <SocialIcon color="#000" label="A" textColor="#fff" />
          </Highlight>
        </div>

        <div style={{ height: 56 }} />

        <Highlight elementId="porscheLogin" activeId={highlightId} style={porscheBtn}>
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <rect width="20" height="20" rx="2" fill="#555" />
            <text x="10" y="14" textAnchor="middle" fontSize="8" fill="#fff">
              P
            </text>
          </svg>
          <span style={{ marginLeft: 10 }}>로그인</span>
        </Highlight>

        <div style={{ height: 56 }} />
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

function SocialIcon({
  color,
  label,
  textColor = "#333",
  border,
}: {
  color: string;
  label: string;
  textColor?: string;
  border?: boolean;
}) {
  return (
    <div
      style={{
        width: 42,
        height: 42,
        borderRadius: "50%",
        background: color,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        fontSize: 14,
        fontWeight: 700,
        color: textColor,
        border: border ? "1px solid #ddd" : undefined,
        boxShadow: border ? "0 1px 3px rgba(0,0,0,0.1)" : undefined,
      }}
    >
      {label}
    </div>
  );
}

const phone: React.CSSProperties = {
  width: 280,
  height: 700,
  background: "#fff",
  borderRadius: 32,
  display: "flex",
  flexDirection: "column",
  boxShadow: "0 2px 40px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.06)",
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  overflow: "hidden",
  margin: "0 auto",
};
const statusBar: React.CSSProperties = {
  height: 44,
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-end",
  padding: "0 20px 6px",
  fontSize: 13,
  color: "#111",
  flexShrink: 0,
};
const content: React.CSSProperties = {
  flex: 1,
  display: "flex",
  flexDirection: "column",
  padding: "0 20px",
  overflow: "auto",
};
const logoArea: React.CSSProperties = {
  flex: 1,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  minHeight: 120,
};
const logoPlaceholder: React.CSSProperties = { marginTop: 40 };
const outlinedBtn: React.CSSProperties = {
  height: 48,
  borderRadius: 10,
  border: "1px solid #4f46e5",
  background: "#fff",
  color: "#4f46e5",
  fontSize: 14,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
};
const primaryBtn: React.CSSProperties = {
  height: 48,
  borderRadius: 10,
  background: "#4f46e5",
  color: "#fff",
  fontSize: 14,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
};
const skipText: React.CSSProperties = {
  textAlign: "center",
  fontSize: 13,
  color: "#9ca3af",
  textDecoration: "underline",
  flexShrink: 0,
};
const dividerRow: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  flexShrink: 0,
};
const dividerLine: React.CSSProperties = { flex: 1, height: 1, background: "#e5e7eb" };
const dividerLabel: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#9ca3af",
  whiteSpace: "nowrap",
};
const socialRow: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  gap: 20,
  flexShrink: 0,
};
const porscheBtn: React.CSSProperties = {
  height: 48,
  borderRadius: 8,
  background: "#333",
  color: "#fff",
  fontSize: 14,
  fontWeight: 700,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
};
