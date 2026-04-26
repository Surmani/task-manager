import { useDroppable } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable'
import { Task } from '../../types'
import TaskCard from './TaskCard'

interface Props {
  status: string
  label: string
  color: string
  tasks: Task[]
  onDelete: (id: number) => void
  onEdit: (task: Task) => void
  onMove: (taskId: number, newStatus: string) => void
}

export default function KanbanColumn({ status, label, color, tasks, onDelete, onEdit, onMove }: Props) {
  const { setNodeRef, isOver } = useDroppable({ id: status })

  return (
    <div
      ref={setNodeRef}
      className={`bg-slate-900 rounded-xl p-4 border-t-4 ${color} min-h-96 transition-colors ${isOver ? 'bg-slate-800' : ''}`}>
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-white font-semibold">{label}</h2>
        <span className="bg-slate-700 text-slate-300 text-xs px-2 py-1 rounded-full">
          {tasks.length}
        </span>
      </div>
      <SortableContext items={tasks.map(t => t.id)} strategy={verticalListSortingStrategy}>
        <div className="space-y-3">
          {tasks.map(task => (
            <TaskCard
              key={task.id}
              task={task}
              onDelete={onDelete}
              onEdit={onEdit}
              onMove={onMove}
            />
          ))}
        </div>
      </SortableContext>
    </div>
  )
}
