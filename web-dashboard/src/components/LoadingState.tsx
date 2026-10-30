interface Props {
  message?: string;
}

export default function LoadingState({ message = "로딩 중..." }: Props) {
  return (
    <div style={container}>
      <div style={spinner} />
      <span style={text}>{message}</span>
    </div>
  );
}

const container: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 10,
  padding: "20px 0",
};

const spinner: React.CSSProperties = {
  width: 18,
  height: 18,
  border: "2.5px solid #e5e7eb",
  borderTop: "2.5px solid #4f46e5",
  borderRadius: "50%",
  animation: "spin 0.7s linear infinite",
};

const text: React.CSSProperties = {
  fontSize: 13,
  color: "#6b7280",
};

if (typeof document !== "undefined" && !document.getElementById("loading-spin")) {
  const style = document.createElement("style");
  style.id = "loading-spin";
  style.textContent = "@keyframes spin { to { transform: rotate(360deg); } }";
  document.head.appendChild(style);
}
