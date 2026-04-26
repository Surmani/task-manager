import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProjects, createProject, deleteProject } from '../api/projects'
import { useAuthStore } from '../store/authStore'
import { Project } from '../types'
import Navbar from '../components/layout/Navbar'
import MembersModal from '../components/project/MembersModal'
import toast from 'react-hot-toast'

export default function ProjectsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [showForm, setShowForm] = useState(false)
  const [managingProject, setManagingProject] = useState<Project | null>(null)
  const [form, setForm] = useState({ name: '', description: '' })
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const isAdmin = user?.role === 'ADMIN'

  useEffect(() => {
    getProjects().then(setProjects).finally(() => setLoading(false))
  }, [])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const project = await createProject({ ...form, memberIds: [] })
      setProjects([...projects, project])
      setForm({ name: '', description: '' })
      setShowForm(false)
      toast.success('Projeto criado!')
    } catch {
      toast.error('Erro ao criar projeto')
    }
  }

  const handleDelete = async (id: number) => {
    if (!confirm('Deletar projeto e todas as tarefas?')) return
    try {
      await deleteProject(id)
      setProjects(projects.filter(p => p.id !== id))
      toast.success('Projeto deletado')
    } catch {
      toast.error('Erro ao deletar')
    }
  }

  const handleMembersUpdate = (updated: Project) => {
    setProjects(projects.map(p => p.id === updated.id ? updated : p))
    setManagingProject(updated)
  }

  return (
    <div className="min-h-screen bg-slate-950">
      <Navbar />
      <div className="max-w-5xl mx-auto p-6">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-2xl font-bold text-white">Meus Projetos</h1>
          {isAdmin && (
            <button
              onClick={() => setShowForm(!showForm)}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors">
              + Novo Projeto
            </button>
          )}
        </div>

        {showForm && isAdmin && (
          <form onSubmit={handleCreate} className="bg-slate-900 p-6 rounded-xl mb-6 space-y-4">
            <input
              placeholder="Nome do projeto"
              value={form.name}
              onChange={e => setForm({ ...form, name: e.target.value })}
              required
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500" />
            <textarea
              placeholder="Descrição"
              value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })}
              className="w-full bg-slate-800 text-white px-4 py-3 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 h-24 resize-none" />
            <div className="flex gap-3">
              <button type="submit"
                className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-lg transition-colors">
                Criar
              </button>
              <button type="button" onClick={() => setShowForm(false)}
                className="bg-slate-700 hover:bg-slate-600 text-white px-6 py-2 rounded-lg transition-colors">
                Cancelar
              </button>
            </div>
          </form>
        )}

        {loading ? (
          <p className="text-slate-400 text-center py-12">Carregando...</p>
        ) : projects.length === 0 ? (
          <p className="text-slate-400 text-center py-12">
            {isAdmin ? 'Nenhum projeto. Crie o primeiro!' : 'Você não foi adicionado a nenhum projeto ainda.'}
          </p>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {projects.map(p => (
              <div key={p.id} className="bg-slate-900 rounded-xl p-5 flex flex-col gap-3">
                <div>
                  <h3 className="text-white font-semibold text-lg">{p.name}</h3>
                  <p className="text-slate-400 text-sm line-clamp-2 mt-1">{p.description}</p>
                </div>
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-xs bg-slate-700 text-slate-300 px-2 py-0.5 rounded">
                    👑 {p.owner.name}
                  </span>
                  <span className="text-xs text-slate-500">
                    {p.members.length} membro(s)
                  </span>
                </div>
                <div className="flex gap-2 mt-auto flex-wrap">
                  <button
                    onClick={() => navigate(`/projects/${p.id}/board`)}
                    className="flex-1 text-sm bg-blue-600 hover:bg-blue-700 text-white px-3 py-2 rounded transition-colors text-center">
                    Abrir Board
                  </button>
                  {isAdmin && p.owner.id === user?.id && (
                    <>
                      <button
                        onClick={() => setManagingProject(p)}
                        className="text-sm bg-slate-600 hover:bg-slate-500 text-white px-3 py-2 rounded transition-colors">
                        👥 Membros
                      </button>
                      <button
                        onClick={() => handleDelete(p.id)}
                        className="text-sm bg-red-700 hover:bg-red-600 text-white px-3 py-2 rounded transition-colors">
                        🗑
                      </button>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {managingProject && (
        <MembersModal
          project={managingProject}
          onClose={() => setManagingProject(null)}
          onUpdate={handleMembersUpdate}
        />
      )}
    </div>
  )
}
