import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import { Task } from '../../types'

const priorityColors: Record<string, string> = {
  LOW: 'bg-slate-600 text-slate-200',
  MEDIUM: 'bg-blue-700 text-blue-100',
  HIGH: 'bg-orange-600 text-orange-100',
  CRITICAL: 'bg-red-600 text-red-100',
}

const priorityLabels: Record<string, string> = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
}

const STATUS_ORDER = ['TODO', 'IN_PROGRESS', 'DONE']

interface Props {
  task: Task
  onDelete: (id: number) => void
  onEdit: (task: Task) => void
  onMove: (taskId: number, newStatus: string) => void
}

export default function TaskCard({ task, onDelete, onEdit, onMove }: Props) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } =
    useSortable({ id: task.id })

  const currentIndex = STATUS_ORDER.indexOf(task.status)
  const canMoveLeft = currentIndex > 0
  const canMoveRight = currentIndex < STATUS_ORDER.length - 1

  return (
    <div className="relative flex items-center gap-1 group">

      {/* Seta esquerda */}
      <button
        onPointerDown={e => e.stopPropagation()}
        onClick={() => canMoveLeft && onMove(task.id, STATUS_ORDER[currentIndex - 1])}
        className={`shrink-0 w-6 min-h-16 flex items-center justify-center rounded-lg transition-colors text-lg
          ${canMoveLeft
            ? 'bg-slate-700 hover:bg-slate-500 text-slate-300 hover:text-white cursor-pointer'
            : 'bg-slate-800/30 text-slate-700 cursor-not-allowed'
          }`}>
        ‹
      </button>

      {/* Card */}
      <div
        ref={setNodeRef}
        style={{ transform: CSS.Transform.toString(transform), transition, opacity: isDragging ? 0.5 : 1 }}
        {...attributes}
        {...listeners}
        className="flex-1 bg-slate-800 rounded-lg p-4 cursor-grab active:cursor-grabbing hover:bg-slate-700 transition-colors">

        <div className="flex items-start justify-between gap-2 mb-2">
          <p className="text-white text-sm font-medium leading-tight flex-1">{task.title}</p>
          <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
            <button
              onPointerDown={e => e.stopPropagation()}
              onClick={e => { e.stopPropagation(); onEdit(task) }}
              className="text-slate-400 hover:text-blue-400 text-xs px-1 transition-colors"
              title="Editar">
              ✏️
            </button>
            <button
              onPointerDown={e => e.stopPropagation()}
              onClick={e => { e.stopPropagation(); onDelete(task.id) }}
              className="text-slate-400 hover:text-red-400 text-xs px-1 transition-colors"
              title="Deletar">
              ✕
            </button>
          </div>
        </div>

        {task.description && (
          <p className="text-slate-400 text-xs mb-3 line-clamp-2">{task.description}</p>
        )}

        <div className="flex items-center justify-between flex-wrap gap-1">
          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${priorityColors[task.priority] || ''}`}>
            {priorityLabels[task.priority] || task.priority}
          </span>
          {task.assignee ? (
            <span className="text-xs text-slate-400 truncate max-w-24" title={task.assignee.name}>
              👤 {task.assignee.name}
            </span>
          ) : (
            <span className="text-xs text-slate-600">Sem responsável</span>
          )}
        </div>

        {task.deadline && (
          <p className="text-xs text-slate-500 mt-2">
            📅 {new Date(task.deadline).toLocaleDateString('pt-BR')}
          </p>
        )}
      </div>

      {/* Seta direita */}
      <button
        onPointerDown={e => e.stopPropagation()}
        onClick={() => canMoveRight && onMove(task.id, STATUS_ORDER[currentIndex + 1])}
        className={`shrink-0 w-6 min-h-16 flex items-center justify-center rounded-lg transition-colors text-lg
          ${canMoveRight
            ? 'bg-slate-700 hover:bg-slate-500 text-slate-300 hover:text-white cursor-pointer'
            : 'bg-slate-800/30 text-slate-700 cursor-not-allowed'
          }`}>
        ›
      </button>
    </div>
  )
}
