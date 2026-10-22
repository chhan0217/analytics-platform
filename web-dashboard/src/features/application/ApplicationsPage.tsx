import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../../api/client";
import type {
  ListResponse,
  Application,
  DataResponse,
  CreatedCredential,
  Credential,
} from "../../api/types";

export default function ApplicationsPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const [apps, setApps] = useState<Application[]>([]);
  const [name, setName] = useState("");
  const [platform, setPlatform] = useState("ANDROID");
  const [credentials, setCredentials] = useState<Record<string, Credential[]>>({});
  const [newKey, setNewKey] = useState<string | null>(null);

  const load = async () => {
    const res = await api<ListResponse<Application>>(`/admin/projects/${projectId}/applications`);
    setApps(res.data);
    for (const app of res.data) {
      const creds = await api<ListResponse<Credential>>(
        `/admin/projects/${projectId}/applications/${app.id}/credentials`,
      );
      setCredentials((prev) => ({ ...prev, [app.id]: creds.data }));
    }
  };

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    void load();
  }, [projectId]); // eslint-disable-line react-hooks/set-state-in-effect

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    await api<DataResponse<Application>>(`/admin/projects/${projectId}/applications`, {
      method: "POST",
      body: JSON.stringify({ name, platform }),
    });
    setName("");
    await load();
  };

  const handleIssueKey = async (appId: string) => {
    const res = await api<DataResponse<CreatedCredential>>(
      `/admin/projects/${projectId}/applications/${appId}/credentials`,
      { method: "POST" },
    );
    setNewKey(res.data.ingestion_key);
    await load();
  };

  const handleRevokeKey = async (appId: string, credId: string) => {
    await api(`/admin/projects/${projectId}/applications/${appId}/credentials/${credId}`, {
      method: "DELETE",
    });
    setNewKey(null);
    await load();
  };

  return (
    <div>
      <h2>앱 관리</h2>
      <form onSubmit={handleCreate} style={{ display: "flex", gap: 8, marginBottom: "1.5rem" }}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="앱 이름"
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd", flex: 1 }}
        />
        <select
          value={platform}
          onChange={(e) => setPlatform(e.target.value)}
          style={{ padding: 8, borderRadius: 4, border: "1px solid #ddd" }}
        >
          <option value="ANDROID">Android</option>
          <option value="IOS">iOS</option>
          <option value="WEB">Web</option>
        </select>
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
          등록
        </button>
      </form>

      {newKey && (
        <div
          style={{
            background: "#ecfdf5",
            border: "1px solid #10b981",
            padding: "1rem",
            borderRadius: 8,
            marginBottom: "1rem",
          }}
        >
          <strong>수집 키가 발급되었습니다 (한 번만 표시):</strong>
          <code
            style={{
              display: "block",
              marginTop: 8,
              padding: 8,
              background: "#fff",
              borderRadius: 4,
              wordBreak: "break-all",
            }}
          >
            {newKey}
          </code>
          <button
            onClick={() => {
              void navigator.clipboard.writeText(newKey);
            }}
            style={{ marginTop: 8, padding: "4px 12px", fontSize: 13, cursor: "pointer" }}
          >
            복사
          </button>
        </div>
      )}

      {apps.map((app) => (
        <div
          key={app.id}
          style={{
            background: "#fff",
            border: "1px solid #e5e7eb",
            borderRadius: 8,
            padding: "1rem",
            marginBottom: "1rem",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <strong>{app.name}</strong>
              <span
                style={{
                  marginLeft: 8,
                  background: "#e0e7ff",
                  padding: "2px 8px",
                  borderRadius: 4,
                  fontSize: 12,
                }}
              >
                {app.platform}
              </span>
              {app.app_identifier && (
                <span style={{ marginLeft: 8, color: "#666", fontSize: 13 }}>
                  {app.app_identifier}
                </span>
              )}
            </div>
            <button
              onClick={() => void handleIssueKey(app.id)}
              style={{
                padding: "4px 12px",
                background: "#10b981",
                color: "#fff",
                border: "none",
                borderRadius: 4,
                cursor: "pointer",
                fontSize: 13,
              }}
            >
              키 발급
            </button>
          </div>
          {credentials[app.id]?.length > 0 && (
            <table
              style={{ width: "100%", marginTop: 8, borderCollapse: "collapse", fontSize: 13 }}
            >
              <thead>
                <tr style={{ borderBottom: "1px solid #e5e7eb" }}>
                  <th style={{ padding: 4, textAlign: "left" }}>Prefix</th>
                  <th style={{ padding: 4, textAlign: "left" }}>상태</th>
                  <th style={{ padding: 4, textAlign: "left" }}>생성일</th>
                  <th style={{ padding: 4 }}></th>
                </tr>
              </thead>
              <tbody>
                {credentials[app.id].map((c) => (
                  <tr key={c.id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                    <td style={{ padding: 4 }}>
                      <code>{c.key_prefix}...</code>
                    </td>
                    <td style={{ padding: 4 }}>
                      <span style={{ color: c.status === "ACTIVE" ? "#10b981" : "#ef4444" }}>
                        {c.status}
                      </span>
                    </td>
                    <td style={{ padding: 4, color: "#666" }}>
                      {new Date(c.created_at).toLocaleString()}
                    </td>
                    <td style={{ padding: 4 }}>
                      {c.status === "ACTIVE" && (
                        <button
                          onClick={() => void handleRevokeKey(app.id, c.id)}
                          style={{
                            color: "#ef4444",
                            background: "none",
                            border: "none",
                            cursor: "pointer",
                            fontSize: 12,
                          }}
                        >
                          폐기
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      ))}
    </div>
  );
}
