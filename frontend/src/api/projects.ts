import api from './client'
import { Project, ProjectReport } from '../types'

export const getProjects = () =>
  api.get<Project[]>('/projects').then(r => r.data)

export const getProject = (id: number) =>
  api.get<Project>(`/projects/${id}`).then(r => r.data)

export const createProject = (data: { name: string; description: string; memberIds: number[] }) =>
  api.post<Project>('/projects', data).then(r => r.data)

export const updateProject = (id: number, data: { name: string; description: string; memberIds: number[] }) =>
  api.put<Project>(`/projects/${id}`, data).then(r => r.data)

export const deleteProject = (id: number) =>
  api.delete(`/projects/${id}`)

export const getReport = (id: number) =>
  api.get<ProjectReport>(`/projects/${id}/report`).then(r => r.data)
