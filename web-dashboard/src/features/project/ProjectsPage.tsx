import { useEffect, useState } from "react";
import { useParams, Link } from "react-router-dom";
import { api } from "../../api/client";
import type { ListResponse, Project, DataResponse } from "../../api/types";

export default function ProjectsPage() {
  const { orgId } = useParams<{ orgId: string }>();
  const [projects, setProjects] = useState<Project[]>([]);
  const [name, setName] = useState("");

  const load = async () => {
    const res = await api<ListResponse<Project>>(`/admin/organizations/${orgId}/projects`);
    setProjects(res.data);
  };

  useEffect(() => {
    void load();
  }, [orgId]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    await api<DataResponse<Project>>(`/admin/organizations/${orgId}/projects`, {
      method: "POST",
      body: JSON.stringify({ name }),
    });
    setName("");
    await load();
  };

  return (
    <div>
      <h2>프로젝트 관리</h2>
      <form onSubmit={handleCreate} style={{ display: "flex", gap: 8, marginBottom: "1.5rem" }}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="프로젝트 이름"
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", flex: 1 }}
        />
        <button
          type="submit"
          style={{
            padding: "8px 16px",
            background: "#4f46e5",
            color: "#fff",
            border: "none",
            borderRadius: 4,
            cursor: "pointer",
          }}
        >
          생성
        </button>
      </form>
      <table style={{ width: "100%", borderCollapse: "collapse" }}>
        <thead>
          <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
            <th style={{ padding: 8 }}>이름</th>
            <th style={{ padding: 8 }}>이벤트 정책</th>
            <th style={{ padding: 8 }}>생성일</th>
            <th style={{ padding: 8 }}>관리</th>
          </tr>
        </thead>
        <tbody>
          {projects.map((p) => (
            <tr key={p.id} style={{ borderBottom: "1px solid #e5e7eb" }}>
              <td style={{ padding: 8 }}>{p.name}</td>
              <td style={{ padding: 8 }}>
                <span
                  style={{
                    background: "#e0e7ff",
                    padding: "2px 8px",
                    borderRadius: 4,
                    fontSize: 12,
                  }}
                >
                  {p.unregistered_event_policy}
                </span>
              </td>
              <td style={{ padding: 8, fontSize: 13, color: "#666" }}>
                {new Date(p.created_at).toLocaleString()}
              </td>
              <td style={{ padding: 8, display: "flex", gap: 8 }}>
                <Link
                  to={`/projects/${p.id}/applications`}
                  style={{ color: "#4f46e5", fontSize: 13 }}
                >
                  앱
                </Link>
                <Link to={`/projects/${p.id}/taxonomy`} style={{ color: "#4f46e5", fontSize: 13 }}>
                  텍소노미
                </Link>
                <Link to={`/projects/${p.id}/events`} style={{ color: "#4f46e5", fontSize: 13 }}>
                  이벤트
                </Link>
                <Link to={`/projects/${p.id}/analytics`} style={{ color: "#4f46e5", fontSize: 13 }}>
                  분석
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
