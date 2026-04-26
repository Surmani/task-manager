import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { DndContext, DragEndEvent, closestCenter } from '@dnd-kit/core'
import { useTaskStore } from '../store/taskStore'
import { createTask, deleteTask, updateTask, searchTasks } from '../api/tasks'
import { getProject, getReport } from '../api/projects'
import { Task, User, ProjectReport } from '../types'
import Navbar from '../components/layout/Navbar'
import KanbanColumn from '../components/task/KanbanColumn'
import TaskFormModal from '../components/task/TaskFormModal'
import TaskEditModal from '../components/task/TaskEditModal'
import toast from 'react-hot-toast'

const COLUMNS = [
  { status: 'TODO', label: 'A Fazer', color: 'border-slate-500' },
  { status: 'IN_PROGRESS', label: 'Em Progresso', color: 'border-blue-500' },
  { status: 'DONE', label: 'Concluído', color: 'border-green-500' },
]

const STATUS_LABELS: Record<string, string> = {
  TODO: 'A Fazer',
  IN_PROGRESS: 'Em Progresso',
  DONE: 'Concluído',
}

const VALID_STATUSES = ['TODO', 'IN_PROGRESS', 'DONE']

export default function BoardPage() {
  const { id } = useParams<{ id: string }>()
  const projectId = Number(id)
  const { tasks, fetchTasks, updateTaskStatus } = useTaskStore()
  const [showForm, setShowForm] = useState(false)
  const [editingTask, setEditingTask] = useState<Task | null>(null)
  const [projectMembers, setProjectMembers] = useState<User[]>([])
  const [showFilters, setShowFilters] = useState(false)
  const [showReport, setShowReport] = useState(false)
  const [report, setReport] = useState<ProjectReport | null>(null)
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState<Task[] | null>(null)
  const [filters, setFilters] = useState({
    status: '', priority: '', assigneeId: '', sortBy: 'createdAt'
  })

  useEffect(() => {
    fetchTasks(projectId)
    getProject(projectId).then(p => {
      setProjectMembers([p.owner, ...p.members.filter(m => m.id !== p.owner.id)])
    }).catch(() => {})
  }, [projectId])

  // Busca textual com debounce
  useEffect(() => {
    if (!search.trim()) { setSearchResults(null); return }
    const timer = setTimeout(async () => {
      try {
        const data = await searchTasks(projectId, search)
        setSearchResults(data.content || [])
      } catch { setSearchResults([]) }
    }, 400)
    return () => clearTimeout(timer)
  }, [search, projectId])

  const handleApplyFilters = () => {
    const params: any = {}
    if (filters.status) params.status = filters.status
    if (filters.priority) params.priority = filters.priority
    if (filters.assigneeId) params.assigneeId = filters.assigneeId
    if (filters.sortBy) params.sortBy = filters.sortBy
    fetchTasks(projectId, params)
    setShowFilters(false)
  }

  const handleClearFilters = () => {
    setFilters({ status: '', priority: '', assigneeId: '', sortBy: 'createdAt' })
    fetchTasks(projectId)
    setShowFilters(false)
  }

  const handleLoadReport = async () => {
    try {
      const data = await getReport(projectId)
      setReport(data)
      setShowReport(true)
    } catch { toast.error('Erro ao carregar relatório') }
  }

  const handleDragEnd = async (event: DragEndEvent) => {
    const { active, over } = event
    if (!over) return
    const overId = String(over.id)
    const newStatus = VALID_STATUSES.includes(overId)
      ? overId
      : tasks.find(t => String(t.id) === overId)?.status
    if (!newStatus) return
    const task = tasks.find(t => String(t.id) === String(active.id))
    if (!task || task.status === newStatus) return
    try {
      await updateTaskStatus(projectId, task.id, newStatus)
      toast.success(`Movido para "${STATUS_LABELS[newStatus]}"`)
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Erro ao mover tarefa')
    }
  }

  const handleMove = async (taskId: number, newStatus: string) => {
    try {
      await updateTaskStatus(projectId, taskId, newStatus)
      toast.success(`Movido para "${STATUS_LABELS[newStatus]}"`)
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Erro ao mover tarefa')
    }
  }

  const handleCreate = async (data: any) => {
    try {
      await createTask(projectId, data)
      await fetchTasks(projectId)
      setShowForm(false)
      toast.success('Tarefa criada!')
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Erro ao criar tarefa')
    }
  }

  const handleEdit = async (data: any) => {
    if (!editingTask) return
    try {
      await updateTask(projectId, editingTask.id, data)
      await fetchTasks(projectId)
      setEditingTask(null)
      toast.success('Tarefa atualizada!')
    } catch (err: any) {
      toast.error(err?.response?.data?.detail || 'Erro ao atualizar tarefa')
    }
  }

  const handleDelete = async (taskId: number) => {
    if (!confirm('Deletar tarefa?')) return
    try {
      await deleteTask(projectId, taskId)
      await fetchTasks(projectId)
      toast.success('Tarefa deletada')
    } catch { toast.error('Erro ao deletar') }
  }

  const displayTasks = searchResults ?? tasks

  return (
    <div className="min-h-screen bg-slate-950">
      <Navbar />
      <div className="p-6">
        {/* Barra de ações */}
        <div className="flex items-center gap-3 mb-6 flex-wrap">
          <input
            placeholder="🔍 Buscar tarefas..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="bg-slate-800 text-white px-4 py-2 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 w-64" />
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`px-4 py-2 rounded-lg text-sm transition-colors ${
              showFilters ? 'bg-blue-600 text-white' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
            }`}>
            ⚙️ Filtros
          </button>
          <button
            onClick={handleLoadReport}
            className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-4 py-2 rounded-lg text-sm transition-colors">
            📊 Relatório
          </button>
          <button
            onClick={() => setShowForm(true)}
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg ml-auto transition-colors">
            + Nova Tarefa
          </button>
        </div>

        {/* Painel de filtros */}
        {showFilters && (
          <div className="bg-slate-900 rounded-xl p-4 mb-6 grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label className="text-slate-400 text-xs mb-1 block">Status</label>
              <select
                value={filters.status}
                onChange={e => setFilters({ ...filters, status: e.target.value })}
                className="w-full bg-slate-800 text-white px-3 py-2 rounded-lg outline-none text-sm">
                <option value="">Todos</option>
                <option value="TODO">A Fazer</option>
                <option value="IN_PROGRESS">Em Progresso</option>
                <option value="DONE">Concluído</option>
              </select>
            </div>
            <div>
              <label className="text-slate-400 text-xs mb-1 block">Prioridade</label>
              <select
                value={filters.priority}
                onChange={e => setFilters({ ...filters, priority: e.target.value })}
                className="w-full bg-slate-800 text-white px-3 py-2 rounded-lg outline-none text-sm">
                <option value="">Todas</option>
                <option value="LOW">Baixa</option>
                <option value="MEDIUM">Média</option>
                <option value="HIGH">Alta</option>
                <option value="CRITICAL">Crítica</option>
              </select>
            </div>
            <div>
              <label className="text-slate-400 text-xs mb-1 block">Responsável</label>
              <select
                value={filters.assigneeId}
                onChange={e => setFilters({ ...filters, assigneeId: e.target.value })}
                className="w-full bg-slate-800 text-white px-3 py-2 rounded-lg outline-none text-sm">
                <option value="">Todos</option>
                {projectMembers.map(m => (
                  <option key={m.id} value={m.id}>{m.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-slate-400 text-xs mb-1 block">Ordenar por</label>
              <select
                value={filters.sortBy}
                onChange={e => setFilters({ ...filters, sortBy: e.target.value })}
                className="w-full bg-slate-800 text-white px-3 py-2 rounded-lg outline-none text-sm">
                <option value="createdAt">Data de criação</option>
                <option value="priority">Prioridade</option>
                <option value="deadline">Prazo</option>
              </select>
            </div>
            <div className="col-span-2 md:col-span-4 flex gap-3">
              <button
                onClick={handleApplyFilters}
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg text-sm transition-colors">
                Aplicar
              </button>
              <button
                onClick={handleClearFilters}
                className="bg-slate-700 hover:bg-slate-600 text-white px-6 py-2 rounded-lg text-sm transition-colors">
                Limpar
              </button>
            </div>
          </div>
        )}

        {/* Aviso de busca ativa */}
        {searchResults !== null && (
          <div className="mb-4 flex items-center gap-3">
            <p className="text-slate-400 text-sm">
              {searchResults.length} resultado(s) para "{search}"
            </p>
            <button
              onClick={() => { setSearch(''); setSearchResults(null) }}
              className="text-xs text-blue-400 hover:underline">
              Limpar busca
            </button>
          </div>
        )}

        {/* Board Kanban */}
        <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {COLUMNS.map(col => (
              <KanbanColumn
                key={col.status}
                {...col}
                tasks={displayTasks.filter(t => t.status === col.status)}
                onDelete={handleDelete}
                onEdit={setEditingTask}
                onMove={handleMove}
              />
            ))}
          </div>
        </DndContext>
      </div>

      {/* Modal criação */}
      {showForm && (
        <TaskFormModal
          members={projectMembers}
          onSubmit={handleCreate}
          onClose={() => setShowForm(false)}
        />
      )}

      {/* Modal edição */}
      {editingTask && (
        <TaskEditModal
          task={editingTask}
          members={projectMembers}
          onSubmit={handleEdit}
          onClose={() => setEditingTask(null)}
        />
      )}

      {/* Modal relatório */}
      {showReport && report && (
        <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
          <div className="bg-slate-900 rounded-2xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-white text-xl font-bold">📊 Relatório do Projeto</h2>
              <button onClick={() => setShowReport(false)} className="text-slate-400 hover:text-white text-2xl">✕</button>
            </div>
            <div className="mb-6">
              <h3 className="text-slate-400 text-xs font-semibold uppercase tracking-wide mb-3">Por Status</h3>
              <div className="space-y-2">
                {Object.entries(report.byStatus).map(([status, count]) => (
                  <div key={status} className="flex items-center justify-between bg-slate-800 px-4 py-2 rounded-lg">
                    <span className="text-slate-300 text-sm">{STATUS_LABELS[status] || status}</span>
                    <span className="text-white font-bold">{count}</span>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <h3 className="text-slate-400 text-xs font-semibold uppercase tracking-wide mb-3">Por Prioridade</h3>
              <div className="space-y-2">
                {Object.entries(report.byPriority).map(([priority, count]) => (
                  <div key={priority} className="flex items-center justify-between bg-slate-800 px-4 py-2 rounded-lg">
                    <span className="text-slate-300 text-sm">{priority}</span>
                    <span className="text-white font-bold">{count}</span>
                  </div>
                ))}
              </div>
            </div>
            <button
              onClick={() => setShowReport(false)}
              className="w-full mt-6 bg-slate-700 hover:bg-slate-600 text-white py-3 rounded-lg font-semibold transition-colors">
              Fechar
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
