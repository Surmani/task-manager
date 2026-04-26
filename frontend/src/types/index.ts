export interface User {
  id: number
  name: string
  email: string
  role: string
}

export interface Project {
  id: number
  name: string
  description: string
  owner: User
  members: User[]
  createdAt: string
}

export interface Task {
  id: number
  title: string
  description: string
  status: string
  priority: string
  deadline: string | null
  createdAt: string
  updatedAt: string
  projectId: number
  assignee: User | null
  creator: User
}

export interface TaskHistory {
  id: number
  field: string
  oldValue: string | null
  newValue: string | null
  changedBy: User
  changedAt: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ProjectReport {
  byStatus: Record<string, number>
  byPriority: Record<string, number>
}
