import { create } from 'zustand'
import { Task } from '../types'
import { getTasks, updateTask, TaskFilters } from '../api/tasks'

interface TaskState {
  tasks: Task[]
  loading: boolean
  fetchTasks: (projectId: number, filters?: TaskFilters) => Promise<void>
  updateTaskStatus: (projectId: number, taskId: number, status: string) => Promise<void>
}

export const useTaskStore = create<TaskState>((set, get) => ({
  tasks: [],
  loading: false,

  fetchTasks: async (projectId, filters) => {
    set({ loading: true })
    try {
      const data = await getTasks(projectId, filters)
      set({ tasks: [...(data.content || [])] })
    } finally {
      set({ loading: false })
    }
  },

  updateTaskStatus: async (projectId, taskId, status) => {
    // Optimistic update — atualiza a UI antes da resposta da API
    set({ tasks: get().tasks.map(t => t.id === taskId ? { ...t, status } : t) })
    try {
      const updatedTask = await updateTask(projectId, taskId, { status })
      // Substitui pelo dado real do servidor (com assignee atualizado, etc)
      set({ tasks: get().tasks.map(t => t.id === taskId ? updatedTask : t) })
    } catch (err) {
      // Reverte buscando do servidor
      const data = await getTasks(projectId)
      set({ tasks: [...(data.content || [])] })
      throw err
    }
  }
}))
