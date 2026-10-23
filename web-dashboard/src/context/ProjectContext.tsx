import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { api } from "../api/client";
import type { Organization, Project } from "../api/types";
import type { ListResponse } from "../api/types";

interface ProjectContextType {
  organizations: Organization[];
  projects: Project[];
  selectedOrg: Organization | null;
  selectedProject: Project | null;
  selectOrg: (org: Organization) => void;
  selectProject: (project: Project) => void;
  loading: boolean;
}

const ProjectContext = createContext<ProjectContextType | null>(null);

export function ProjectProvider({ children }: { children: React.ReactNode }) {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [selectedProject, setSelectedProject] = useState<Project | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api<ListResponse<Organization>>("/admin/organizations")
      .then((res) => {
        setOrganizations(res.data);
        const savedOrgId = localStorage.getItem("selected_org_id");
        const saved = res.data.find((o) => o.id === savedOrgId);
        if (saved) setSelectedOrg(saved);
        else if (res.data.length === 1) setSelectedOrg(res.data[0]);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (!selectedOrg) {
      setProjects([]);
      return;
    }
    localStorage.setItem("selected_org_id", selectedOrg.id);
    api<ListResponse<Project>>(`/admin/organizations/${selectedOrg.id}/projects`).then((res) => {
      setProjects(res.data);
      const savedProjId = localStorage.getItem("selected_project_id");
      const saved = res.data.find((p) => p.id === savedProjId);
      if (saved) setSelectedProject(saved);
      else if (res.data.length === 1) setSelectedProject(res.data[0]);
      else setSelectedProject(null);
    });
  }, [selectedOrg]);

  const selectOrg = useCallback((org: Organization) => {
    setSelectedOrg(org);
    setSelectedProject(null);
  }, []);

  const selectProject = useCallback((project: Project) => {
    setSelectedProject(project);
    localStorage.setItem("selected_project_id", project.id);
  }, []);

  return (
    <ProjectContext.Provider
      value={{
        organizations,
        projects,
        selectedOrg,
        selectedProject,
        selectOrg,
        selectProject,
        loading,
      }}
    >
      {children}
    </ProjectContext.Provider>
  );
}

export function useProject() {
  const ctx = useContext(ProjectContext);
  if (!ctx) throw new Error("useProject must be used within ProjectProvider");
  return ctx;
}
