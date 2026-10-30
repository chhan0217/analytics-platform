import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function ResetPasswordScreen({ highlightId }: ScreenProps) {
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
        <div style={title}>비밀번호 재설정</div>
        <div style={{ height: 8 }} />
        <div style={desc}>(소셜 계정을 통해 가입한 고객은 해당 소셜 계정으로 로그인해주세요)</div>
        <div style={{ height: 32 }} />

        <div style={tabRow}>
          <Highlight elementId="tabEmail" activeId={highlightId} style={tabActive}>
            이메일로 찾기
          </Highlight>
          <Highlight elementId="tabPass" activeId={highlightId} style={tabInactive}>
            휴대폰 본인인증하기
          </Highlight>
        </div>
        <div style={{ height: 32 }} />

        <div style={label}>이메일</div>
        <div style={emailRow}>
          <Highlight
            elementId="emailIdInput"
            activeId={highlightId}
            style={{ ...inputField, flex: 1 }}
          >
            <span style={placeholder}>이메일</span>
          </Highlight>
          <span style={atSymbol}>@</span>
          <Highlight
            elementId="emailDomainInput"
            activeId={highlightId}
            style={{ ...inputField, flex: 1 }}
          >
            <span style={placeholder}>직접입력 ∨</span>
          </Highlight>
        </div>
        <div style={{ height: 8 }} />

        <div style={inputRow}>
          <Highlight
            elementId="authInput"
            activeId={highlightId}
            style={{ ...inputField, flex: 1 }}
          >
            <span style={placeholder}>인증번호 6자리</span>
          </Highlight>
          <Highlight elementId="sendCodeButton" activeId={highlightId} style={sendBtn}>
            인증요청
          </Highlight>
        </div>
      </div>

      <div style={bottomBar}>
        <Highlight elementId="nextButton" activeId={highlightId} style={disabledBtn}>
          다음
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
const desc: React.CSSProperties = { fontSize: 12, color: "#333", lineHeight: 1.6 };
const tabRow: React.CSSProperties = {
  display: "flex",
  borderBottom: "2px solid #e5e7eb",
  flexShrink: 0,
};
const tabActive: React.CSSProperties = {
  flex: 1,
  textAlign: "center",
  fontSize: 12,
  fontWeight: 700,
  color: "#111",
  padding: "10px 0",
  borderBottom: "2px solid #111",
  marginBottom: -2,
};
const tabInactive: React.CSSProperties = {
  flex: 1,
  textAlign: "center",
  fontSize: 12,
  fontWeight: 500,
  color: "#9ca3af",
  padding: "10px 0",
};
const label: React.CSSProperties = {
  fontSize: 12,
  fontWeight: 600,
  color: "#333",
  marginBottom: 6,
};
const emailRow: React.CSSProperties = { display: "flex", alignItems: "center", gap: 6 };
const inputField: React.CSSProperties = {
  height: 48,
  border: "1px solid #d1d5db",
  borderRadius: 8,
  padding: "0 12px",
  display: "flex",
  alignItems: "center",
  fontSize: 13,
  boxSizing: "border-box",
};
const placeholder: React.CSSProperties = { color: "#9ca3af", fontSize: 13 };
const atSymbol: React.CSSProperties = { fontSize: 16, fontWeight: 600, color: "#333" };
const inputRow: React.CSSProperties = { display: "flex", gap: 8 };
const sendBtn: React.CSSProperties = {
  height: 48,
  padding: "0 14px",
  borderRadius: 8,
  background: "#4f46e5",
  color: "#fff",
  fontSize: 12,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  whiteSpace: "nowrap",
  flexShrink: 0,
};
const bottomBar: React.CSSProperties = { padding: "12px 20px 32px", flexShrink: 0 };
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
};
