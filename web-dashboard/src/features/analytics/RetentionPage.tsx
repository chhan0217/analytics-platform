import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { DataResponse, RetentionResponse } from "../../api/types";
import { downloadCsv } from "../../utils/csv";

const today = new Date();
const defaultStart = new Date(today.getTime() - 30 * 86400000).toISOString().slice(0, 10);
const defaultEnd = today.toISOString().slice(0, 10);

function rateColor(rate: number): string {
  if (rate >= 0.5) return "#15803d";
  if (rate >= 0.3) return "#22c55e";
  if (rate >= 0.15) return "#86efac";
  if (rate >= 0.05) return "#bbf7d0";
  if (rate > 0) return "#f0fdf4";
  return "transparent";
}

function rateBg(rate: number): string {
  if (rate >= 0.5) return "#dcfce7";
  if (rate >= 0.3) return "#dcfce7";
  if (rate >= 0.15) return "#f0fdf4";
  if (rate >= 0.05) return "#f0fdf4";
  if (rate > 0) return "#fafafa";
  return "transparent";
}

export default function RetentionPage() {
  const { selectedProject } = useProject();
  const [startDate, setStartDate] = useState(defaultStart);
  const [endDate, setEndDate] = useState(defaultEnd);
  const [type, setType] = useState<"new" | "active">("new");
  const [retentionDays, setRetentionDays] = useState("1,7,30");
  const [data, setData] = useState<RetentionResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const load = async () => {
    if (!selectedProject) return;
    setLoading(true);
    try {
      const res = await api<DataResponse<RetentionResponse>>(
        `/admin/projects/${selectedProject.id}/analytics/retention?start_date=${startDate}&end_date=${endDate}&type=${type}&retention_days=${retentionDays}`,
      );
      setData(res.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [selectedProject?.id]);

  const days = retentionDays
    .split(",")
    .map((d) => parseInt(d.trim(), 10))
    .filter((d) => !isNaN(d));

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>리텐션 분석</h2>

      {/* 필터 */}
      <div
        style={{
          display: "flex",
          gap: 12,
          marginBottom: "1.5rem",
          alignItems: "center",
          flexWrap: "wrap",
        }}
      >
        <label style={{ fontSize: 13 }}>
          타입:
          <select
            value={type}
            onChange={(e) => setType(e.target.value as "new" | "active")}
            style={{
              marginLeft: 4,
              padding: "4px 8px",
              borderRadius: 4,
              border: "1px solid #d1d5db",
            }}
          >
            <option value="new">신규 사용자</option>
            <option value="active">활성 사용자</option>
          </select>
        </label>
        <label style={{ fontSize: 13 }}>
          시작:
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            style={{
              marginLeft: 4,
              padding: "4px 8px",
              borderRadius: 4,
              border: "1px solid #d1d5db",
            }}
          />
        </label>
        <label style={{ fontSize: 13 }}>
          종료:
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            style={{
              marginLeft: 4,
              padding: "4px 8px",
              borderRadius: 4,
              border: "1px solid #d1d5db",
            }}
          />
        </label>
        <label style={{ fontSize: 13 }}>
          D-N:
          <input
            value={retentionDays}
            onChange={(e) => setRetentionDays(e.target.value)}
            placeholder="1,7,30"
            style={{
              marginLeft: 4,
              padding: "4px 8px",
              borderRadius: 4,
              border: "1px solid #d1d5db",
              width: 120,
            }}
          />
        </label>
        <button
          onClick={load}
          disabled={loading}
          style={{
            padding: "6px 16px",
            background: "#4f46e5",
            color: "#fff",
            border: "none",
            borderRadius: 4,
            cursor: "pointer",
            fontSize: 13,
          }}
        >
          {loading ? "조회 중..." : "조회"}
        </button>
        {data && (
          <button
            onClick={() => {
              const headers = [
                "코호트 날짜",
                "코호트 크기",
                ...days.map((d) => `D${d} 수`),
                ...days.map((d) => `D${d} 비율`),
              ];
              const rows = data.cohorts.map((c) => [
                c.date,
                c.cohort_size,
                ...days.map((d) => c.retention[String(d)]?.count ?? ""),
                ...days.map((d) =>
                  c.retention[String(d)]
                    ? (c.retention[String(d)].rate * 100).toFixed(1) + "%"
                    : "",
                ),
              ]);
              downloadCsv(`retention_${type}_${startDate}_${endDate}.csv`, headers, rows);
            }}
            style={{
              padding: "6px 16px",
              background: "#059669",
              color: "#fff",
              border: "none",
              borderRadius: 4,
              cursor: "pointer",
              fontSize: 13,
              marginLeft: 8,
            }}
          >
            CSV
          </button>
        )}
      </div>

      {/* 설명 */}
      <p style={{ fontSize: 13, color: "#6b7280", margin: "0 0 1rem" }}>
        {type === "new"
          ? "해당 기간에 처음 방문한 사용자가 D-N일 후에 다시 사용한 비율"
          : "해당 날짜에 활동한 모든 사용자가 D-N일 후에 다시 사용한 비율"}
      </p>

      {/* 히트맵 테이블 */}
      {data && (
        <div style={{ overflowX: "auto" }}>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                <th style={{ padding: 8 }}>코호트</th>
                <th style={{ padding: 8, textAlign: "right" }}>
                  {type === "new" ? "신규" : "활성"}
                </th>
                {days.map((d) => (
                  <th key={d} style={{ padding: 8, textAlign: "center" }}>
                    D{d}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {data.cohorts.map((cohort) => (
                <tr key={cohort.date} style={{ borderBottom: "1px solid #e5e7eb" }}>
                  <td style={{ padding: 8, whiteSpace: "nowrap" }}>{cohort.date}</td>
                  <td style={{ padding: 8, textAlign: "right", fontWeight: 600 }}>
                    {cohort.cohort_size}명
                  </td>
                  {days.map((d) => {
                    const ret = cohort.retention[String(d)];
                    if (!ret) {
                      return (
                        <td key={d} style={{ padding: 8, textAlign: "center", color: "#d1d5db" }}>
                          -
                        </td>
                      );
                    }
                    return (
                      <td
                        key={d}
                        style={{
                          padding: 8,
                          textAlign: "center",
                          background: rateBg(ret.rate),
                          color: rateColor(ret.rate),
                          fontWeight: 600,
                        }}
                        title={`${ret.count}명 / ${cohort.cohort_size}명`}
                      >
                        {cohort.cohort_size > 0 ? `${(ret.rate * 100).toFixed(1)}%` : "-"}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
