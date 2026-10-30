interface Props {
  icon?: string;
  title: string;
  description?: string;
}

export default function EmptyState({ icon = "📊", title, description }: Props) {
  return (
    <div style={container}>
      <div style={iconStyle}>{icon}</div>
      <div style={titleStyle}>{title}</div>
      {description && <div style={descStyle}>{description}</div>}
    </div>
  );
}

const container: React.CSSProperties = {
  textAlign: "center",
  padding: "48px 20px",
};

const iconStyle: React.CSSProperties = {
  fontSize: 36,
  marginBottom: 12,
  opacity: 0.3,
};

const titleStyle: React.CSSProperties = {
  fontSize: 15,
  color: "#6b7280",
  fontWeight: 500,
};

const descStyle: React.CSSProperties = {
  fontSize: 13,
  color: "#9ca3af",
  marginTop: 4,
};
