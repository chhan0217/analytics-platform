import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setAuthToken } from "../../api/client";
import type { DataResponse, LoginResponse } from "../../api/types";

export default function LoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("admin@analytics.local");
  const [password, setPassword] = useState("admin1234");
  const [error, setError] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    try {
      const res = await api<DataResponse<LoginResponse>>("/admin/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });
      setAuthToken(res.data.token);
      navigate("/");
    } catch {
      setError("로그인 실패. 이메일과 비밀번호를 확인하세요.");
    }
  };

  return (
    <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh", background: "#f5f5f5" }}>
      <form onSubmit={handleLogin} style={{ background: "#fff", padding: "2rem", borderRadius: 8, boxShadow: "0 2px 8px rgba(0,0,0,0.1)", width: 360 }}>
        <h2 style={{ marginBottom: "1.5rem", textAlign: "center" }}>Analytics Platform</h2>
        {error && <p style={{ color: "red", marginBottom: "1rem", fontSize: 14 }}>{error}</p>}
        <div style={{ marginBottom: "1rem" }}>
          <label style={{ display: "block", marginBottom: 4, fontSize: 14 }}>이메일</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} style={{ width: "100%", padding: 8, borderRadius: 4, border: "1px solid #ddd", boxSizing: "border-box" }} />
        </div>
        <div style={{ marginBottom: "1.5rem" }}>
          <label style={{ display: "block", marginBottom: 4, fontSize: 14 }}>비밀번호</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} style={{ width: "100%", padding: 8, borderRadius: 4, border: "1px solid #ddd", boxSizing: "border-box" }} />
        </div>
        <button type="submit" style={{ width: "100%", padding: 10, background: "#4f46e5", color: "#fff", border: "none", borderRadius: 4, cursor: "pointer", fontSize: 16 }}>로그인</button>
      </form>
    </div>
  );
}
