import { useEffect, useState } from "react";
import { formatKstDateTime } from "../../utils/date";
import { api } from "../../api/client";
import { useProject } from "../../context/ProjectContext";
import type { ListResponse, Organization, DataResponse } from "../../api/types";

export default function OrganizationsPage() {
  const [orgs, setOrgs] = useState<Organization[]>([]);
  const [name, setName] = useState("");
  const { selectedOrg, selectOrg, reloadOrgs } = useProject();

  const load = async () => {
    const res = await api<ListResponse<Organization>>("/admin/organizations");
    setOrgs(res.data);
  };

  useEffect(() => {
    void load();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    const res = await api<DataResponse<Organization>>("/admin/organizations", {
      method: "POST",
      body: JSON.stringify({ name }),
    });
    setName("");
    await load();
    await reloadOrgs();
    selectOrg(res.data);
  };

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>조직 관리</h2>
      <form onSubmit={handleCreate} style={{ display: "flex", gap: 8, marginBottom: "1.5rem" }}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="조직 이름"
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
            <th style={{ padding: 8 }}>생성일</th>
            <th style={{ padding: 8 }}>작업</th>
          </tr>
        </thead>
        <tbody>
          {orgs.map((org) => (
            <tr key={org.id} style={{ borderBottom: "1px solid #e5e7eb" }}>
              <td style={{ padding: 8 }}>{org.name}</td>
              <td style={{ padding: 8, fontSize: 13, color: "#666" }}>
                {formatKstDateTime(org.created_at)}
              </td>
              <td style={{ padding: 8 }}>
                {selectedOrg?.id === org.id ? (
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
                    onClick={() => selectOrg(org)}
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
