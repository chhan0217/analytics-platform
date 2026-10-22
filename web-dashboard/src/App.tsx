import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { getAuthToken } from "./api/client";
import Layout from "./components/Layout";
import LoginPage from "./features/auth/LoginPage";
import OrganizationsPage from "./features/organization/OrganizationsPage";
import ProjectsPage from "./features/project/ProjectsPage";
import ApplicationsPage from "./features/application/ApplicationsPage";
import TaxonomyPage from "./features/taxonomy/TaxonomyPage";
import EventsPage from "./features/events/EventsPage";
import UsersPage from "./features/users/UsersPage";
import AnalyticsPage from "./features/analytics/AnalyticsPage";

function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = getAuthToken();
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <RequireAuth>
              <Layout />
            </RequireAuth>
          }
        >
          <Route path="/" element={<OrganizationsPage />} />
          <Route path="/organizations/:orgId/projects" element={<ProjectsPage />} />
          <Route path="/projects/:projectId/applications" element={<ApplicationsPage />} />
          <Route path="/projects/:projectId/taxonomy" element={<TaxonomyPage />} />
          <Route path="/projects/:projectId/events" element={<EventsPage />} />
          <Route path="/projects/:projectId/analytics" element={<AnalyticsPage />} />
          <Route
            path="/events"
            element={<div style={{ color: "#666" }}>프로젝트를 선택한 후 이벤트를 조회하세요.</div>}
          />
          <Route
            path="/taxonomy"
            element={
              <div style={{ color: "#666" }}>프로젝트를 선택한 후 텍소노미를 관리하세요.</div>
            }
          />
          <Route path="/users" element={<UsersPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
