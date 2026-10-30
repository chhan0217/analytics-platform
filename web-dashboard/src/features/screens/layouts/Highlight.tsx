import { useEffect, useState } from "react";

interface Props {
  elementId: string;
  activeId?: string | null;
  children: React.ReactNode;
  onClick?: () => void;
  style?: React.CSSProperties;
}

export default function Highlight({ elementId, activeId, children, onClick, style }: Props) {
  const [flash, setFlash] = useState(false);
  const isActive = activeId === elementId;

  useEffect(() => {
    if (isActive) {
      setFlash(true);
      const timer = setTimeout(() => setFlash(false), 600);
      return () => clearTimeout(timer);
    }
  }, [isActive, activeId]);

  return (
    <div
      data-element-id={elementId}
      onClick={onClick}
      style={{
        ...style,
        position: "relative",
        cursor: onClick ? "pointer" : undefined,
      }}
    >
      {children}
      {flash && <div style={pulseOverlay} />}
    </div>
  );
}

const pulseOverlay: React.CSSProperties = {
  position: "absolute",
  inset: -2,
  borderRadius: "inherit",
  border: "2px solid #4f46e5",
  boxShadow: "0 0 12px rgba(79,70,229,0.4)",
  pointerEvents: "none",
  animation: "highlightPulse 0.6s ease-out forwards",
};

// CSS 애니메이션을 글로벌로 주입
if (typeof document !== "undefined" && !document.getElementById("highlight-styles")) {
  const style = document.createElement("style");
  style.id = "highlight-styles";
  style.textContent = `
    @keyframes highlightPulse {
      0% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.8; transform: scale(1.02); }
      100% { opacity: 0; transform: scale(1.05); }
    }
  `;
  document.head.appendChild(style);
}
