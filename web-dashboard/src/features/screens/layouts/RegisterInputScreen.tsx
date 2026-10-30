import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

export default function RegisterInputScreen({ highlightId }: ScreenProps) {
  return (
    <div style={phone}>
      <StatusBar />
      <div style={appBar}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#333" strokeWidth="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </div>

      <div style={progressTrack}>
        <div style={{ ...progressFill, width: "75%" }} />
      </div>

      <div style={content}>
        <div style={{ height: 40 }} />
        <div style={title}>계정정보 확인</div>
        <div style={{ height: 40 }} />

        <div style={label}>이름</div>
        <div style={disabledField}>홍길동</div>
        <div style={{ height: 32 }} />

        <div style={label}>휴대폰번호</div>
        <div style={disabledField}>01012345678</div>
        <div style={{ height: 32 }} />

        <div style={labelRequired}>이메일 <span style={{ color: "#ef4444" }}>*</span></div>
        <div style={emailRow}>
          <Highlight elementId="emailInput" activeId={highlightId} style={{ ...inputField, flex: 1 }}>
            <span style={placeholder}>이메일</span>
          </Highlight>
          <span style={atSymbol}>@</span>
          <Highlight elementId="emailDomain" activeId={highlightId} style={{ ...inputField, flex: 1 }}>
            <span style={placeholder}>직접입력 ∨</span>
          </Highlight>
        </div>
        <div style={{ height: 24 }} />

        <div style={labelRequired}>비밀번호 <span style={{ color: "#ef4444" }}>*</span></div>
        <Highlight elementId="passwordInput" activeId={highlightId} style={inputField}>
          <span style={placeholder}>8자 이상/영문 대소문자, 숫자, 특수문자</span>
        </Highlight>
        <div style={{ height: 12 }} />
        <Highlight elementId="passwordConfirm" activeId={highlightId} style={inputField}>
          <span style={placeholder}>비밀번호 재입력</span>
        </Highlight>
        <div style={{ height: 12 }} />

        <div style={labelRequired}>결제수단 등록 <span style={{ color: "#ef4444" }}>*</span></div>
        <div style={{ height: 8 }} />
        <Highlight elementId="paymentRegister" activeId={highlightId} style={paymentCard}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" strokeWidth="2">
            <path d="M12 5v14M5 12h14" />
          </svg>
          <div style={{ fontSize: 11, color: "#9ca3af", marginTop: 6 }}>대표 결제수단으로 등록됩니다</div>
          <div style={paymentBtn}>결제수단 등록</div>
        </Highlight>
        <div style={{ height: 12 }} />

        <div style={label}>친구 초대 코드</div>
        <Highlight elementId="referralInput" activeId={highlightId} style={inputField}>
          <span style={placeholder}>초대 코드</span>
        </Highlight>
        <div style={{ height: 32 }} />
      </div>

      <div style={bottomBar}>
        <Highlight elementId="registerButton" activeId={highlightId} style={disabledBtn}>
          가입하기
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
const label: React.CSSProperties = { fontSize: 12, fontWeight: 600, color: "#333", marginBottom: 6 };
const labelRequired: React.CSSProperties = { ...label };
const disabledField: React.CSSProperties = { height: 48, border: "1px solid #e5e7eb", borderRadius: 8, padding: "0 12px", display: "flex", alignItems: "center", fontSize: 13, color: "#9ca3af", background: "#f9fafb", boxSizing: "border-box" };
const emailRow: React.CSSProperties = { display: "flex", alignItems: "center", gap: 6 };
const inputField: React.CSSProperties = { height: 48, border: "1px solid #d1d5db", borderRadius: 8, padding: "0 12px", display: "flex", alignItems: "center", fontSize: 13, boxSizing: "border-box" };
const placeholder: React.CSSProperties = { color: "#9ca3af", fontSize: 12 };
const atSymbol: React.CSSProperties = { fontSize: 16, fontWeight: 600, color: "#333" };
const paymentCard: React.CSSProperties = { border: "1px solid #e5e7eb", borderRadius: 8, background: "#f9fafb", padding: "20px 0", display: "flex", flexDirection: "column", alignItems: "center" };
const paymentBtn: React.CSSProperties = { marginTop: 10, padding: "8px 20px", borderRadius: 100, border: "1px solid #d1d5db", background: "#fff", fontSize: 12, fontWeight: 700, color: "#333" };
const bottomBar: React.CSSProperties = { padding: "12px 20px 32px", flexShrink: 0 };
const disabledBtn: React.CSSProperties = { height: 48, borderRadius: 10, background: "#d1d5db", color: "#9ca3af", fontSize: 14, fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "center" };
