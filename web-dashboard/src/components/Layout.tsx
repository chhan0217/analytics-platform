import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { setAuthToken } from "../api/client";

const navItems = [
  { to: "/", label: "조직" },
  { to: "/events", label: "이벤트" },
  { to: "/taxonomy", label: "텍소노미" },
  { to: "/users", label: "사용자" },
];

export default function Layout() {
  const navigate = useNavigate();

  const handleLogout = () => {
    setAuthToken(null);
    navigate("/login");
  };

  return (
    <div style={{ display: "flex", height: "100vh" }}>
      <nav style={{ width: 220, background: "#1e1b4b", color: "#fff", padding: "1rem", display: "flex", flexDirection: "column" }}>
        <h3 style={{ marginBottom: "2rem", fontSize: 16 }}>Analytics Platform</h3>
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            style={({ isActive }) => ({
              display: "block",
              padding: "0.5rem 0.75rem",
              marginBottom: 4,
              borderRadius: 4,
              color: "#fff",
              textDecoration: "none",
              background: isActive ? "rgba(255,255,255,0.15)" : "transparent",
              fontSize: 14,
            })}
          >
            {item.label}
          </NavLink>
        ))}
        <div style={{ marginTop: "auto" }}>
          <button onClick={handleLogout} style={{ background: "none", border: "1px solid rgba(255,255,255,0.3)", color: "#fff", padding: "0.5rem 0.75rem", borderRadius: 4, cursor: "pointer", width: "100%", fontSize: 14 }}>
            로그아웃
          </button>
        </div>
      </nav>
      <main style={{ flex: 1, overflow: "auto", padding: "1.5rem", background: "#f9fafb" }}>
        <Outlet />
      </main>
    </div>
  );
}
