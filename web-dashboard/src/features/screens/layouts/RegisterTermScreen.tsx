import { useState } from "react";
import type { ScreenProps } from "./index";
import Highlight from "./Highlight";

const TERMS = [
  { id: "term1", label: "서비스 이용 약관", required: true },
  { id: "term2", label: "개인정보 수집·이용 동의", required: true },
  { id: "term3", label: "개인정보 제3자 제공 동의", required: true },
  { id: "term4", label: "위치기반 서비스 이용약관", required: true },
  { id: "term5", label: "마케팅 수신 동의", required: false },
];

export default function RegisterTermScreen({ highlightId }: ScreenProps) {
  const [checked, setChecked] = useState<Set<string>>(new Set());

  const allChecked = TERMS.every((t) => checked.has(t.id));
  const requiredAllChecked = TERMS.filter((t) => t.required).every((t) => checked.has(t.id));

  const toggleAll = () => {
    if (allChecked) {
      setChecked(new Set());
    } else {
      setChecked(new Set(TERMS.map((t) => t.id)));
    }
  };

  const toggle = (id: string) => {
    setChecked((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <div style={phone}>
      <StatusBar />
      <div style={appBar}>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#333" strokeWidth="2">
          <path d="M15 18l-6-6 6-6" />
        </svg>
      </div>

      <div style={progressTrack}>
        <div style={{ ...progressFill, width: "25%" }} />
      </div>

      <div style={content}>
        <div style={{ height: 40 }} />
        <div style={title}>약관동의</div>
        <div style={{ height: 40 }} />

        <Highlight elementId="allAgree" activeId={highlightId} style={checkRow} onClick={toggleAll}>
          <Checkbox checked={allChecked} />
          <span style={allAgreeText}>전체 동의</span>
        </Highlight>

        <div style={{ height: 24 }} />
        <div style={divider} />
        <div style={{ height: 16 }} />

        {TERMS.map((term) => (
          <Highlight
            key={term.id}
            elementId={term.id}
            activeId={highlightId}
            style={termRow}
            onClick={() => toggle(term.id)}
          >
            <Checkbox checked={checked.has(term.id)} />
            <span style={termLabel}>
              {term.label} <span style={{ textDecoration: "underline" }}>약관내용</span>{" "}
              <span style={{ color: "#9ca3af" }}>({term.required ? "필수" : "선택"})</span>
            </span>
          </Highlight>
        ))}
      </div>

      <div style={bottomBar}>
        <Highlight elementId="nextButton" activeId={highlightId} style={requiredAllChecked ? primaryBtn : disabledBtn}>
          다음
        </Highlight>
      </div>
    </div>
  );
}

function Checkbox({ checked = false }: { checked?: boolean }) {
  return (
    <div
      style={{
        width: 22,
        height: 22,
        borderRadius: 6,
        border: checked ? "none" : "2px solid #d1d5db",
        background: checked ? "#4f46e5" : "#fff",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        flexShrink: 0,
        transition: "all 0.15s ease",
        cursor: "pointer",
      }}
    >
      {checked && (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="3">
          <path d="M20 6L9 17l-5-5" />
        </svg>
      )}
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
const checkRow: React.CSSProperties = { display: "flex", alignItems: "center", gap: 12, cursor: "pointer" };
const allAgreeText: React.CSSProperties = { fontSize: 16, fontWeight: 800, color: "#111" };
const divider: React.CSSProperties = { height: 1, background: "#e5e7eb" };
const termRow: React.CSSProperties = { display: "flex", alignItems: "center", gap: 12, padding: "8px 0", cursor: "pointer" };
const termLabel: React.CSSProperties = { fontSize: 12, color: "#333", lineHeight: 1.6 };
const bottomBar: React.CSSProperties = { padding: "12px 20px 32px", flexShrink: 0 };
const primaryBtn: React.CSSProperties = { height: 48, borderRadius: 10, background: "#4f46e5", color: "#fff", fontSize: 14, fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "center", transition: "all 0.2s ease" };
const disabledBtn: React.CSSProperties = { height: 48, borderRadius: 10, background: "#d1d5db", color: "#9ca3af", fontSize: 14, fontWeight: 600, display: "flex", alignItems: "center", justifyContent: "center", transition: "all 0.2s ease" };
