import type { ScreenProps } from "./index";

export default function SplashScreen(_props: ScreenProps) {
  return (
    <div style={phone}>
      <div style={content}>
        <svg width="120" height="40" viewBox="0 0 120 40" fill="none">
          <rect width="120" height="40" rx="6" fill="rgba(255,255,255,0.15)" />
          <text x="60" y="26" textAnchor="middle" fontSize="16" fontWeight="700" fill="#fff">
            CHAEVI
          </text>
        </svg>
      </div>
    </div>
  );
}

const phone: React.CSSProperties = {
  width: 280,
  height: 700,
  background: "linear-gradient(135deg, #4f46e5 0%, #6366f1 100%)",
  borderRadius: 32,
  display: "flex",
  flexDirection: "column",
  boxShadow: "0 2px 40px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.06)",
  fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  overflow: "hidden",
  margin: "0 auto",
};

const content: React.CSSProperties = {
  flex: 1,
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
};
