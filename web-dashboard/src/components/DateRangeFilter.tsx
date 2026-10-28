import { todayKst, daysAgoKst } from "../utils/date";

interface DateRangeFilterProps {
  startDate: string;
  endDate: string;
  onStartDateChange: (v: string) => void;
  onEndDateChange: (v: string) => void;
  onSearch: () => void;
  loading?: boolean;
  children?: React.ReactNode;
}

const presets = [
  { label: "오늘", days: 0 },
  { label: "7일", days: 7 },
  { label: "30일", days: 30 },
  { label: "90일", days: 90 },
];

export default function DateRangeFilter({
  startDate,
  endDate,
  onStartDateChange,
  onEndDateChange,
  onSearch,
  loading,
  children,
}: DateRangeFilterProps) {
  const applyPreset = (days: number) => {
    onStartDateChange(daysAgoKst(days));
    onEndDateChange(todayKst());
  };

  return (
    <div style={container}>
      <div style={presetRow}>
        {presets.map((p) => (
          <button
            key={p.days}
            onClick={() => applyPreset(p.days)}
            style={presetBtn}
          >
            {p.label}
          </button>
        ))}
      </div>
      <input
        type="date"
        value={startDate}
        onChange={(e) => onStartDateChange(e.target.value)}
        style={inputStyle}
      />
      <span style={{ color: "#9ca3af" }}>~</span>
      <input
        type="date"
        value={endDate}
        onChange={(e) => onEndDateChange(e.target.value)}
        style={inputStyle}
      />
      {children}
      <button onClick={onSearch} disabled={loading} style={btnStyle}>
        {loading ? "조회 중..." : "조회"}
      </button>
    </div>
  );
}

const container: React.CSSProperties = {
  display: "flex",
  gap: 8,
  marginBottom: "1.5rem",
  alignItems: "center",
  flexWrap: "wrap",
};

const presetRow: React.CSSProperties = {
  display: "flex",
  gap: 4,
};

const presetBtn: React.CSSProperties = {
  padding: "4px 10px",
  background: "#f3f4f6",
  color: "#374151",
  border: "1px solid #e5e7eb",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 13,
};

const inputStyle: React.CSSProperties = {
  padding: "6px 10px",
  borderRadius: 6,
  border: "1px solid #d1d5db",
  fontSize: 14,
};

const btnStyle: React.CSSProperties = {
  padding: "6px 16px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 6,
  cursor: "pointer",
  fontSize: 14,
  fontWeight: 500,
};
