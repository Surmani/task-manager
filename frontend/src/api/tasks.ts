import api from './client'
import { Task, PageResponse, TaskHistory } from '../types'

export interface TaskFilters {
  status?: string
  priority?: string
  assigneeId?: number
  sortBy?: string
  page?: number
  size?: number
}

export const getTasks = (projectId: number, filters: TaskFilters = {}) =>
  api.get<PageResponse<Task>>(`/projects/${projectId}/tasks`, { params: filters }).then(r => r.data)

export const searchTasks = (projectId: number, q: string, page = 0) =>
  api.get<PageResponse<Task>>(`/projects/${projectId}/tasks/search`, { params: { q, page } }).then(r => r.data)

export const createTask = (projectId: number, data: object) =>
  api.post<Task>(`/projects/${projectId}/tasks`, data).then(r => r.data)

export const updateTask = (projectId: number, taskId: number, data: object) =>
  api.patch<Task>(`/projects/${projectId}/tasks/${taskId}`, data).then(r => r.data)

export const deleteTask = (projectId: number, taskId: number) =>
  api.delete(`/projects/${projectId}/tasks/${taskId}`)

export const getTaskHistory = (projectId: number, taskId: number) =>
  api.get<TaskHistory[]>(`/projects/${projectId}/tasks/${taskId}/history`).then(r => r.data)
