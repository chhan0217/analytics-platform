interface Props {
  message: string;
  onRetry?: () => void;
}

export default function ErrorState({ message, onRetry }: Props) {
  return (
    <div style={container}>
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#ef4444" strokeWidth="2">
        <circle cx="12" cy="12" r="10" />
        <line x1="12" y1="8" x2="12" y2="12" />
        <line x1="12" y1="16" x2="12.01" y2="16" />
      </svg>
      <span style={text}>{message}</span>
      {onRetry && (
        <button onClick={onRetry} style={retryBtn}>
          다시 시도
        </button>
      )}
    </div>
  );
}

const container: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 8,
  padding: "12px 16px",
  background: "#fef2f2",
  borderRadius: 8,
  border: "1px solid #fecaca",
};

const text: React.CSSProperties = {
  fontSize: 13,
  color: "#dc2626",
  flex: 1,
};

const retryBtn: React.CSSProperties = {
  padding: "4px 12px",
  background: "#fff",
  color: "#dc2626",
  border: "1px solid #fecaca",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 12,
  fontWeight: 500,
  whiteSpace: "nowrap",
};
