import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { getAuthToken } from "./api/client";
import { ProjectProvider } from "./context/ProjectContext";
import Layout from "./components/Layout";
import LoginPage from "./features/auth/LoginPage";
import OverviewPage from "./features/overview/OverviewPage";
import ApplicationsPage from "./features/application/ApplicationsPage";
import TaxonomyPage from "./features/taxonomy/TaxonomyPage";
import EventsPage from "./features/events/EventsPage";
import EventSummaryPage from "./features/events/EventSummaryPage";
import UsersPage from "./features/users/UsersPage";
import AnalyticsPage from "./features/analytics/AnalyticsPage";
import RetentionPage from "./features/analytics/RetentionPage";
import OrganizationsPage from "./features/organization/OrganizationsPage";
import ProjectsPage from "./features/project/ProjectsPage";

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
              <ProjectProvider>
                <Layout />
              </ProjectProvider>
            </RequireAuth>
          }
        >
          <Route path="/" element={<Navigate to="/overview" replace />} />
          <Route path="/overview" element={<OverviewPage />} />
          <Route path="/events" element={<EventsPage />} />
          <Route path="/events/summary" element={<EventSummaryPage />} />
          <Route path="/screens" element={<AnalyticsPage tab="screens" />} />
          <Route path="/funnels" element={<AnalyticsPage tab="funnels" />} />
          <Route path="/journeys" element={<AnalyticsPage tab="journeys" />} />
          <Route path="/retention" element={<RetentionPage />} />
          <Route path="/users" element={<UsersPage />} />
          <Route path="/settings/organizations" element={<OrganizationsPage />} />
          <Route path="/settings/projects" element={<ProjectsPage />} />
          <Route path="/settings/apps" element={<ApplicationsPage />} />
          <Route path="/settings/taxonomy" element={<TaxonomyPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
