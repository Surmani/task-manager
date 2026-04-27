import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { DndContext } from '@dnd-kit/core'
import { SortableContext } from '@dnd-kit/sortable'
import TaskCard from '../../components/task/TaskCard'
import { Task } from '../../types'

// Mock do dnd-kit para evitar erros de ambiente jsdom
vi.mock('@dnd-kit/sortable', async () => {
  const actual = await vi.importActual('@dnd-kit/sortable')
  return {
    ...actual,
    useSortable: () => ({
      attributes: {},
      listeners: {},
      setNodeRef: vi.fn(),
      transform: null,
      transition: null,
      isDragging: false,
    }),
  }
})

const mockTask: Task = {
  id: 1,
  title: 'Corrigir bug no login',
  description: 'Erro ao autenticar com OAuth',
  status: 'TODO',
  priority: 'HIGH',
  deadline: '2026-12-31T23:59:00',
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
  projectId: 1,
  assignee: {
    id: 2,
    name: 'Lucas',
    email: 'lucas@test.com',
    role: 'MEMBER',
  },
  creator: {
    id: 1,
    name: 'Admin',
    email: 'admin@test.com',
    role: 'ADMIN',
  },
}

function renderCard(task: Task, overrides = {}) {
  const onDelete = vi.fn()
  const onEdit = vi.fn()
  const onMove = vi.fn()

  render(
    <DndContext>
      <SortableContext items={[task.id]}>
        <TaskCard
          task={task}
          onDelete={onDelete}
          onEdit={onEdit}
          onMove={onMove}
          {...overrides}
        />
      </SortableContext>
    </DndContext>
  )

  return { onDelete, onEdit, onMove }
}

describe('TaskCard', () => {
  it('should render task title', () => {
    renderCard(mockTask)
    expect(screen.getByText('Corrigir bug no login')).toBeInTheDocument()
  })

  it('should render task description', () => {
    renderCard(mockTask)
    expect(screen.getByText('Erro ao autenticar com OAuth')).toBeInTheDocument()
  })

  it('should render priority badge', () => {
    renderCard(mockTask)
    expect(screen.getByText('Alta')).toBeInTheDocument()
  })

  it('should render assignee name', () => {
    renderCard(mockTask)
    expect(screen.getByText(/Lucas/)).toBeInTheDocument()
  })

  it('should render deadline formatted in pt-BR', () => {
    renderCard(mockTask)
    expect(screen.getByText(/31\/12\/2026/)).toBeInTheDocument()
  })

  it('should show "Sem responsável" when no assignee', () => {
    renderCard({ ...mockTask, assignee: null })
    expect(screen.getByText('Sem responsável')).toBeInTheDocument()
  })

  it('should call onEdit when edit button is clicked', () => {
    const { onEdit } = renderCard(mockTask)
    const editBtn = screen.getByTitle('Editar')
    fireEvent.click(editBtn)
    expect(onEdit).toHaveBeenCalledWith(mockTask)
  })

  it('should call onDelete when delete button is clicked', () => {
    const { onDelete } = renderCard(mockTask)
    const deleteBtn = screen.getByTitle('Deletar')
    fireEvent.click(deleteBtn)
    expect(onDelete).toHaveBeenCalledWith(mockTask.id)
  })

  it('should disable left arrow when task is TODO (first column)', () => {
    renderCard({ ...mockTask, status: 'TODO' })
    const leftArrow = screen.getByText('‹')
    expect(leftArrow).toHaveClass('cursor-not-allowed')
  })

  it('should disable right arrow when task is DONE (last column)', () => {
    renderCard({ ...mockTask, status: 'DONE' })
    const rightArrow = screen.getByText('›')
    expect(rightArrow).toHaveClass('cursor-not-allowed')
  })

  it('should call onMove with previous status when left arrow is clicked', () => {
    const { onMove } = renderCard({ ...mockTask, status: 'IN_PROGRESS' })
    const leftArrow = screen.getByText('‹')
    fireEvent.click(leftArrow)
    expect(onMove).toHaveBeenCalledWith(mockTask.id, 'TODO')
  })

  it('should call onMove with next status when right arrow is clicked', () => {
    const { onMove } = renderCard({ ...mockTask, status: 'IN_PROGRESS' })
    const rightArrow = screen.getByText('›')
    fireEvent.click(rightArrow)
    expect(onMove).toHaveBeenCalledWith(mockTask.id, 'DONE')
  })
})
