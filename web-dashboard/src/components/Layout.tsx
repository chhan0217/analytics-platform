import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { setAuthToken } from "../api/client";
import { useProject } from "../context/ProjectContext";

const analysisItems = [
  { to: "/overview", label: "개요" },
  { to: "/events", label: "이벤트" },
  { to: "/screens", label: "화면 분석" },
  { to: "/funnels", label: "퍼널" },
  { to: "/journeys", label: "여정" },
  { to: "/users", label: "사용자" },
];

const settingsItems = [
  { to: "/settings/apps", label: "앱 관리" },
  { to: "/settings/taxonomy", label: "텍소노미" },
];

function navStyle({ isActive }: { isActive: boolean }) {
  return {
    display: "block" as const,
    padding: "0.4rem 0.75rem",
    marginBottom: 2,
    borderRadius: 4,
    color: "#fff",
    textDecoration: "none" as const,
    background: isActive ? "rgba(255,255,255,0.15)" : "transparent",
    fontSize: 14,
  };
}

export default function Layout() {
  const navigate = useNavigate();
  const { organizations, projects, selectedOrg, selectedProject, selectOrg, selectProject } =
    useProject();

  const handleLogout = () => {
    setAuthToken(null);
    navigate("/login");
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", height: "100vh" }}>
      {/* 헤더 */}
      <header
        style={{
          background: "#1e1b4b",
          color: "#fff",
          padding: "0.5rem 1rem",
          display: "flex",
          alignItems: "center",
          gap: 12,
          flexShrink: 0,
        }}
      >
        <span style={{ fontWeight: 700, fontSize: 15, marginRight: 16 }}>Analytics Platform</span>

        <select
          value={selectedOrg?.id ?? ""}
          onChange={(e) => {
            const org = organizations.find((o) => o.id === e.target.value);
            if (org) selectOrg(org);
          }}
          style={{
            padding: "4px 8px",
            borderRadius: 4,
            border: "1px solid rgba(255,255,255,0.3)",
            background: "rgba(255,255,255,0.1)",
            color: "#fff",
            fontSize: 13,
          }}
        >
          <option value="" style={{ color: "#333" }}>
            조직 선택
          </option>
          {organizations.map((o) => (
            <option key={o.id} value={o.id} style={{ color: "#333" }}>
              {o.name}
            </option>
          ))}
        </select>

        <span style={{ color: "rgba(255,255,255,0.4)" }}>/</span>

        <select
          value={selectedProject?.id ?? ""}
          onChange={(e) => {
            const proj = projects.find((p) => p.id === e.target.value);
            if (proj) selectProject(proj);
          }}
          disabled={!selectedOrg}
          style={{
            padding: "4px 8px",
            borderRadius: 4,
            border: "1px solid rgba(255,255,255,0.3)",
            background: selectedOrg ? "rgba(255,255,255,0.1)" : "rgba(255,255,255,0.05)",
            color: selectedOrg ? "#fff" : "rgba(255,255,255,0.4)",
            fontSize: 13,
          }}
        >
          <option value="" style={{ color: "#333" }}>
            프로젝트 선택
          </option>
          {projects.map((p) => (
            <option key={p.id} value={p.id} style={{ color: "#333" }}>
              {p.name}
            </option>
          ))}
        </select>

        <div style={{ marginLeft: "auto" }}>
          <button
            onClick={handleLogout}
            style={{
              background: "none",
              border: "1px solid rgba(255,255,255,0.3)",
              color: "#fff",
              padding: "4px 12px",
              borderRadius: 4,
              cursor: "pointer",
              fontSize: 13,
            }}
          >
            로그아웃
          </button>
        </div>
      </header>

      <div style={{ display: "flex", flex: 1, overflow: "hidden" }}>
        {/* 사이드바 */}
        <nav
          style={{
            width: 200,
            background: "#312e81",
            color: "#fff",
            padding: "0.75rem",
            display: "flex",
            flexDirection: "column",
            overflow: "auto",
            flexShrink: 0,
          }}
        >
          <div style={{ fontSize: 11, color: "rgba(255,255,255,0.5)", marginBottom: 4 }}>분석</div>
          {analysisItems.map((item) => (
            <NavLink key={item.to} to={item.to} style={navStyle}>
              {item.label}
            </NavLink>
          ))}

          <div
            style={{ fontSize: 11, color: "rgba(255,255,255,0.5)", marginTop: 16, marginBottom: 4 }}
          >
            설정
          </div>
          {settingsItems.map((item) => (
            <NavLink key={item.to} to={item.to} style={navStyle}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        {/* 메인 콘텐츠 */}
        <main style={{ flex: 1, overflow: "auto", padding: "1.5rem", background: "#f9fafb" }}>
          {!selectedProject ? (
            <div
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                height: "100%",
                color: "#9ca3af",
                fontSize: 15,
              }}
            >
              상단에서 조직과 프로젝트를 선택하세요
            </div>
          ) : (
            <Outlet />
          )}
        </main>
      </div>
    </div>
  );
}
