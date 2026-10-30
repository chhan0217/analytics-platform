import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function LoginScreen({ highlightId }: ScreenProps) {
  return (
    <div style={phone}>
      <StatusBar />
      <div style={appBar}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#333" strokeWidth="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </div>

      <div style={content}>
        <div style={{ height: 32 }} />
        <div style={title}>로그인</div>
        <div style={{ height: 48 }} />

        <div style={emailRow}>
          <Highlight
            elementId="emailInput"
            activeId={highlightId}
            style={{ ...inputField, flex: 1 }}
          >
            <span style={placeholder}>이메일</span>
          </Highlight>
          <span style={atSymbol}>@</span>
          <Highlight
            elementId="emailDomainSelector"
            activeId={highlightId}
            style={{ ...inputField, flex: 1 }}
          >
            <span style={placeholder}>직접입력 ∨</span>
          </Highlight>
        </div>

        <div style={{ height: 20 }} />

        <Highlight elementId="passwordInput" activeId={highlightId} style={inputField}>
          <span style={placeholder}>비밀번호</span>
          <svg
            width="18"
            height="18"
            viewBox="0 0 24 24"
            fill="none"
            stroke="#bbb"
            strokeWidth="1.5"
          >
            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
            <circle cx="12" cy="12" r="3" />
          </svg>
        </Highlight>

        <div style={{ height: 32 }} />

        <Highlight elementId="loginButton" activeId={highlightId} style={disabledBtn}>
          로그인
        </Highlight>

        <div style={{ height: 24 }} />

        <div style={linksRow}>
          <Highlight elementId="signupLink" activeId={highlightId}>
            <span style={linkText}>회원가입</span>
          </Highlight>
          <div style={verticalDivider} />
          <Highlight elementId="findIdLink" activeId={highlightId}>
            <span style={linkText}>아이디 찾기</span>
          </Highlight>
          <div style={verticalDivider} />
          <Highlight elementId="resetPasswordLink" activeId={highlightId}>
            <span style={linkText}>비밀번호 재설정</span>
          </Highlight>
        </div>

        <div style={{ flex: 1 }} />

        <div style={corpSection}>
          <div style={corpNotice}>*법인 회원 가입은 채비 웹사이트를 통해서만 가능합니다</div>
          <div style={{ height: 24 }} />
          <Highlight elementId="chaeviWebsiteButton" activeId={highlightId}>
            <div style={corpBtn}>채비 웹사이트 이동</div>
          </Highlight>
        </div>
        <div style={{ height: 48 }} />
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
const appBar: React.CSSProperties = {
  height: 48,
  display: "flex",
  alignItems: "center",
  padding: "0 12px",
  flexShrink: 0,
  borderBottom: "1px solid #f0f0f0",
};
const content: React.CSSProperties = {
  flex: 1,
  display: "flex",
  flexDirection: "column",
  padding: "0 20px",
  overflow: "auto",
};
const title: React.CSSProperties = { fontSize: 22, fontWeight: 800, color: "#111" };
const emailRow: React.CSSProperties = { display: "flex", alignItems: "center", gap: 6 };
const inputField: React.CSSProperties = {
  height: 48,
  border: "1px solid #d1d5db",
  borderRadius: 8,
  padding: "0 12px",
  display: "flex",
  alignItems: "center",
  justifyContent: "space-between",
  fontSize: 13,
  boxSizing: "border-box",
};
const placeholder: React.CSSProperties = { color: "#9ca3af", fontSize: 13 };
const atSymbol: React.CSSProperties = { fontSize: 16, fontWeight: 600, color: "#333" };
const disabledBtn: React.CSSProperties = {
  height: 48,
  borderRadius: 10,
  background: "#d1d5db",
  color: "#9ca3af",
  fontSize: 14,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  flexShrink: 0,
};
const linksRow: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  gap: 12,
};
const linkText: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#6b7280",
  whiteSpace: "nowrap",
};
const verticalDivider: React.CSSProperties = { width: 1, height: 14, background: "#d1d5db" };
const corpSection: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
};
const corpNotice: React.CSSProperties = { fontSize: 11, color: "#9ca3af", textAlign: "center" };
const corpBtn: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: 100,
  border: "1px solid #d1d5db",
  background: "#fff",
  color: "#333",
  fontSize: 12,
  fontWeight: 700,
};
