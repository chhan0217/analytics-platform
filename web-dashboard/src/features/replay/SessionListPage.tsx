import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { todayKst, daysAgoKst, formatKstDateTime } from "../../utils/date";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { ListResponse, SessionListEntry } from "../../api/types";
import DateRangeFilter from "../../components/DateRangeFilter";
import LoadingState from "../../components/LoadingState";
import ErrorState from "../../components/ErrorState";
import EmptyState from "../../components/EmptyState";

function formatDuration(startedAt: string, endedAt: string | null, lastEventAt: string): string {
  const end = endedAt || lastEventAt;
  const ms = new Date(end).getTime() - new Date(startedAt).getTime();
  if (ms < 0) return "-";
  const sec = Math.floor(ms / 1000);
  if (sec < 60) return `${sec}초`;
  const min = Math.floor(sec / 60);
  const remainSec = sec % 60;
  if (min < 60) return `${min}분 ${remainSec}초`;
  const hours = Math.floor(min / 60);
  return `${hours}시간 ${min % 60}분`;
}

export default function SessionListPage() {
  const { selectedProject } = useProject();
  const projectId = selectedProject?.id;
  const navigate = useNavigate();

  const [startDate, setStartDate] = useState(() => daysAgoKst(7));
  const [endDate, setEndDate] = useState(() => todayKst());
  const [sessions, setSessions] = useState<SessionListEntry[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pageSize = 20;

  const load = async (p: number = page) => {
    if (!projectId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api<ListResponse<SessionListEntry>>(
        `/admin/projects/${projectId}/sessions?start_date=${startDate}&end_date=${endDate}&page=${p}&size=${pageSize}`,
      );
      setSessions(res.data);
      setTotal(res.meta.total);
      setPage(p);
    } catch {
      setError("세션 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const totalPages = Math.ceil(total / pageSize);

  return (
    <div>
      <h2 style={titleStyle}>세션 리플레이</h2>

      <DateRangeFilter
        startDate={startDate}
        endDate={endDate}
        onStartDateChange={setStartDate}
        onEndDateChange={setEndDate}
        onSearch={() => load(1)}
        loading={loading}
      />

      {loading && <LoadingState />}
      {error && <ErrorState message={error} onRetry={() => load(1)} />}

      {sessions.length === 0 && !loading && !error && (
        <EmptyState
          icon="🔍"
          title="세션이 없습니다"
          description="조회 버튼을 눌러 세션을 검색하세요."
        />
      )}

      {sessions.length > 0 && (
        <>
          <div style={{ fontSize: 13, color: "#6b7280", marginBottom: 8 }}>전체 {total}개 세션</div>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>시작 시간</th>
                <th style={thStyle}>사용자</th>
                <th style={thStyle}>디바이스</th>
                <th style={thStyle}>이벤트</th>
                <th style={thStyle}>화면</th>
                <th style={thStyle}>세션 시간</th>
                <th style={thStyle}></th>
              </tr>
            </thead>
            <tbody>
              {sessions.map((s) => (
                <tr key={s.session_id} style={trStyle}>
                  <td style={tdStyle}>{formatKstDateTime(s.started_at)}</td>
                  <td style={tdStyle}>
                    <span style={{ fontSize: 12, color: "#6b7280" }}>
                      {s.external_user_id || s.anonymous_id.slice(0, 8) + "..."}
                    </span>
                  </td>
                  <td style={tdStyle}>
                    {s.device ? (
                      <span style={{ fontSize: 12 }}>
                        {s.device.device_model} ({s.device.os_name} {s.device.os_version})
                      </span>
                    ) : (
                      "-"
                    )}
                  </td>
                  <td style={{ ...tdStyle, textAlign: "center" }}>{s.event_count}</td>
                  <td style={{ ...tdStyle, textAlign: "center" }}>{s.screen_count}</td>
                  <td style={tdStyle}>
                    {formatDuration(s.started_at, s.ended_at, s.last_event_at)}
                  </td>
                  <td style={tdStyle}>
                    <button
                      onClick={() => navigate(`/sessions/${s.session_id}/replay`)}
                      style={viewBtn}
                    >
                      타임라인
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {totalPages > 1 && (
            <div style={paginationStyle}>
              <button onClick={() => load(page - 1)} disabled={page <= 1} style={pageBtn}>
                이전
              </button>
              <span style={{ fontSize: 13, color: "#6b7280" }}>
                {page} / {totalPages}
              </span>
              <button onClick={() => load(page + 1)} disabled={page >= totalPages} style={pageBtn}>
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}

const titleStyle: React.CSSProperties = {
  fontSize: 20,
  fontWeight: 600,
  color: "#1e1b4b",
  marginBottom: "1rem",
};

const tableStyle: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  background: "#fff",
  borderRadius: 8,
  overflow: "hidden",
  boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
};

const thStyle: React.CSSProperties = {
  textAlign: "left",
  padding: "10px 12px",
  fontSize: 12,
  fontWeight: 600,
  color: "#6b7280",
  borderBottom: "1px solid #e5e7eb",
  background: "#f9fafb",
};

const tdStyle: React.CSSProperties = {
  padding: "10px 12px",
  fontSize: 13,
  borderBottom: "1px solid #f3f4f6",
};

const trStyle: React.CSSProperties = {
  cursor: "pointer",
};

const viewBtn: React.CSSProperties = {
  padding: "4px 12px",
  background: "#4f46e5",
  color: "#fff",
  border: "none",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 12,
};

const paginationStyle: React.CSSProperties = {
  display: "flex",
  gap: 12,
  alignItems: "center",
  justifyContent: "center",
  marginTop: 16,
};

const pageBtn: React.CSSProperties = {
  padding: "4px 12px",
  background: "#f3f4f6",
  border: "1px solid #e5e7eb",
  borderRadius: 4,
  cursor: "pointer",
  fontSize: 13,
};
