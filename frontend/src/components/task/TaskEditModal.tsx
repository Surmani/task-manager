import { useState } from 'react'
import { Task, User } from '../../types'

interface Props {
  task: Task
  members: User[]
  onSubmit: (data: any) => void
  onClose: () => void
}

export default function TaskEditModal({ task, members, onSubmit, onClose }: Props) {
  const [form, setForm] = useState({
    title: task.title || '',
    description: task.description || '',
    priority: task.priority || 'MEDIUM',
    assigneeId: task.assignee?.id ? String(task.assignee.id) : '',
    deadlineDate: task.deadline ? task.deadline.split('T')[0] : '',
    deadlineTime: task.deadline ? task.deadline.split('T')[1]?.slice(0, 5) : '',
    useTime: !!task.deadline && task.deadline.split('T')[1]?.slice(0, 5) !== '23:59',
  })
  const [error, setError] = useState('')

  const priorityLabels: Record<string, string> = {
    LOW: 'Baixa', MEDIUM: 'Média', HIGH: 'Alta', CRITICAL: 'Crítica',
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    let deadline: string | null = null
    if (form.deadlineDate) {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const selected = new Date(form.deadlineDate + 'T00:00:00')
      if (selected < today) {
        setError('A data limite não pode ser no passado.')
        return
      }
      deadline = form.useTime && form.deadlineTime
        ? `${form.deadlineDate}T${form.deadlineTime}:00`
        : `${form.deadlineDate}T23:59:00`
    }

    const hadAssignee = !!task.assignee
    const hasAssignee = form.assigneeId !== ''

    onSubmit({
      title: form.title,
      description: form.description,
      priority: form.priority,
      deadline,
      removeAssignee: hadAssignee && !hasAssignee ? true : undefined,
      assigneeId: hasAssignee ? Number(form.assigneeId) : undefined,
    })
  }

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 rounded-2xl p-6 w-full max-w-md max-h-screen overflow-y-auto">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-white text-xl font-bold">Editar Tarefa</h2>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-2xl">✕</button>
        </div>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-slate-400 text-sm mb-1 block">Título *</label>
            <input
              value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              required
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          <div>
            <label className="text-slate-400 text-sm mb-1 block">Descrição</label>
            <textarea
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 h-24 resize-none" />
          </div>
          <div>
            <label className="text-slate-400 text-sm mb-1 block">Prioridade</label>
            <select
              value={form.priority}
              onChange={e => setForm({ ...form, priority: e.target.value })}
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500">
              {Object.entries(priorityLabels).map(([val, label]) => (
                <option key={val} value={val}>{label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-slate-400 text-sm mb-1 block">Responsável</label>
            <select
              value={form.assigneeId}
              onChange={e => setForm({ ...form, assigneeId: e.target.value })}
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500">
              <option value="">Sem responsável</option>
              {members.map(m => (
                <option key={m.id} value={m.id}>{m.name} ({m.role})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-slate-400 text-sm mb-1 block">
              Data limite <span className="text-slate-500">(opcional)</span>
            </label>
            <input
              type="date"
              value={form.deadlineDate}
              onChange={e => setForm({ ...form, deadlineDate: e.target.value })}
              min={new Date().toISOString().split('T')[0]}
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500" />
          </div>
          {form.deadlineDate && (
            <div>
              <label className="flex items-center gap-2 text-slate-400 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.useTime}
                  onChange={e => setForm({ ...form, useTime: e.target.checked })} />
                Definir horário específico
              </label>
              {form.useTime && (
                <div className="mt-2 flex gap-3 items-center">
                  <div className="flex-1">
                    <label className="text-slate-500 text-xs mb-1 block">Hora (0-23)</label>
                    <input
                      type="number" min="0" max="23" placeholder="HH"
                      value={form.deadlineTime.split(':')[0] || ''}
                      onChange={e => {
                        const h = String(e.target.value).padStart(2, '0')
                        const m = form.deadlineTime.split(':')[1] || '00'
                        setForm({ ...form, deadlineTime: `${h}:${m}` })
                      }}
                      className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 text-center" />
                  </div>
                  <span className="text-white text-xl mt-4">:</span>
                  <div className="flex-1">
                    <label className="text-slate-500 text-xs mb-1 block">Minuto (0-59)</label>
                    <input
                      type="number" min="0" max="59" placeholder="MM"
                      value={form.deadlineTime.split(':')[1] || ''}
                      onChange={e => {
                        const h = form.deadlineTime.split(':')[0] || '00'
                        const m = String(e.target.value).padStart(2, '0')
                        setForm({ ...form, deadlineTime: `${h}:${m}` })
                      }}
                      className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 text-center" />
                  </div>
                </div>
              )}
            </div>
          )}
          {error && (
            <p className="text-red-400 text-sm bg-red-900/20 px-3 py-2 rounded-lg">{error}</p>
          )}
          <div className="flex gap-3 pt-2">
            <button type="submit"
              className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-3 rounded-lg font-semibold transition-colors">
              Salvar
            </button>
            <button type="button" onClick={onClose}
              className="flex-1 bg-slate-700 hover:bg-slate-600 text-white py-3 rounded-lg font-semibold transition-colors">
              Cancelar
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
