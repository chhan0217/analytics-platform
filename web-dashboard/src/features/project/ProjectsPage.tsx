import { useEffect, useState } from "react";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { ListResponse, Project, DataResponse } from "../../api/types";

export default function ProjectsPage() {
  const { selectedOrg, selectedProject, selectProject, reloadOrgs } = useProject();
  const [projects, setProjects] = useState<Project[]>([]);
  const [name, setName] = useState("");

  const load = async () => {
    if (!selectedOrg) return;
    const res = await api<ListResponse<Project>>(`/admin/organizations/${selectedOrg.id}/projects`);
    setProjects(res.data);
  };

  useEffect(() => {
    void load();
  }, [selectedOrg?.id]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !selectedOrg) return;
    const res = await api<DataResponse<Project>>(
      `/admin/organizations/${selectedOrg.id}/projects`,
      {
        method: "POST",
        body: JSON.stringify({ name }),
      },
    );
    setName("");
    await load();
    await reloadOrgs();
    selectProject(res.data);
  };

  if (!selectedOrg) {
    return (
      <div style={{ color: "#9ca3af", textAlign: "center", marginTop: "3rem" }}>
        먼저 상단에서 조직을 선택하세요.
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>
        프로젝트 관리
        <span style={{ fontSize: 14, color: "#6b7280", fontWeight: 400, marginLeft: 8 }}>
          ({selectedOrg.name})
        </span>
      </h2>
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
            <th style={{ padding: 8 }}>작업</th>
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
              <td style={{ padding: 8 }}>
                {selectedProject?.id === p.id ? (
                  <span
                    style={{
                      background: "#4f46e5",
                      color: "#fff",
                      padding: "4px 12px",
                      borderRadius: 4,
                      fontSize: 13,
                    }}
                  >
                    선택됨
                  </span>
                ) : (
                  <button
                    onClick={() => selectProject(p)}
                    style={{
                      background: "none",
                      border: "1px solid #4f46e5",
                      color: "#4f46e5",
                      padding: "4px 12px",
                      borderRadius: 4,
                      cursor: "pointer",
                      fontSize: 13,
                    }}
                  >
                    선택
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
