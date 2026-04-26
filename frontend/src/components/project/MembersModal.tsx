import { useState } from 'react'
import { updateProject } from '../../api/projects'
import { searchUsers } from '../../api/users'
import { Project, User } from '../../types'
import toast from 'react-hot-toast'

interface Props {
  project: Project
  onClose: () => void
  onUpdate: (updated: Project) => void
}

export default function MembersModal({ project, onClose, onUpdate }: Props) {
  const [search, setSearch] = useState('')
  const [searchResults, setSearchResults] = useState<User[]>([])
  const [searching, setSearching] = useState(false)

  const handleSearch = async () => {
    if (!search.trim()) return
    setSearching(true)
    try {
      const results = await searchUsers(search)
      setSearchResults(results.filter(u => u.id !== project.owner.id))
    } catch {
      toast.error('Erro ao buscar usuários')
    } finally {
      setSearching(false)
    }
  }

  const handleToggleMember = async (userId: number) => {
    const currentIds = project.members.map(m => m.id)
    const newIds = currentIds.includes(userId)
      ? currentIds.filter(id => id !== userId)
      : [...currentIds, userId]
    try {
      const updated = await updateProject(project.id, {
        name: project.name,
        description: project.description,
        memberIds: newIds,
      })
      onUpdate(updated)
      toast.success(currentIds.includes(userId) ? 'Membro removido' : 'Membro adicionado!')
    } catch {
      toast.error('Erro ao atualizar membros')
    }
  }

  const isMember = (userId: number) =>
    project.members.some(m => m.id === userId)

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 rounded-2xl p-6 w-full max-w-lg">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-white text-xl font-bold">Gerenciar Membros</h2>
            <p className="text-slate-400 text-sm mt-1">{project.name}</p>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white text-2xl">✕</button>
        </div>

        {/* Dono do projeto */}
        <div className="mb-4">
          <h3 className="text-slate-400 text-xs font-semibold uppercase tracking-wide mb-2">Dono</h3>
          <div className="flex items-center justify-between bg-slate-800 px-4 py-3 rounded-lg">
            <div>
              <p className="text-white text-sm font-medium">{project.owner.name}</p>
              <p className="text-slate-400 text-xs">{project.owner.email}</p>
            </div>
            <span className="text-xs bg-blue-700 px-2 py-0.5 rounded text-blue-100">Dono</span>
          </div>
        </div>

        {/* Membros atuais */}
        <div className="mb-6">
          <h3 className="text-slate-400 text-xs font-semibold uppercase tracking-wide mb-2">
            Membros ({project.members.length})
          </h3>
          {project.members.length === 0 ? (
            <p className="text-slate-500 text-sm">Nenhum membro adicionado ainda.</p>
          ) : (
            <div className="space-y-2 max-h-40 overflow-y-auto">
              {project.members.map(m => (
                <div key={m.id} className="flex items-center justify-between bg-slate-800 px-4 py-2 rounded-lg">
                  <div>
                    <p className="text-white text-sm font-medium">{m.name}</p>
                    <p className="text-slate-400 text-xs">{m.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs bg-slate-700 px-2 py-0.5 rounded">{m.role}</span>
                    <button
                      onClick={() => handleToggleMember(m.id)}
                      className="text-xs bg-red-700 hover:bg-red-600 text-white px-2 py-1 rounded transition-colors">
                      Remover
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Busca de novos membros */}
        <div>
          <h3 className="text-slate-400 text-xs font-semibold uppercase tracking-wide mb-2">
            Adicionar membro
          </h3>
          <div className="flex gap-2 mb-3">
            <input
              placeholder="Buscar por nome ou e-mail..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && handleSearch()}
              className="flex-1 bg-slate-800 text-white px-4 py-2 rounded-lg outline-none focus:ring-2 focus:ring-blue-500 text-sm" />
            <button
              onClick={handleSearch}
              disabled={searching}
              className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm transition-colors disabled:opacity-50">
              {searching ? '...' : 'Buscar'}
            </button>
          </div>

          {searchResults.length > 0 && (
            <div className="space-y-2 max-h-48 overflow-y-auto">
              {searchResults.map(u => (
                <div key={u.id} className="flex items-center justify-between bg-slate-800 px-4 py-2 rounded-lg">
                  <div>
                    <p className="text-white text-sm font-medium">{u.name}</p>
                    <p className="text-slate-400 text-xs">{u.email} · {u.role}</p>
                  </div>
                  <button
                    onClick={() => handleToggleMember(u.id)}
                    className={`text-xs px-3 py-1 rounded font-medium transition-colors ${
                      isMember(u.id)
                        ? 'bg-red-700 hover:bg-red-600 text-white'
                        : 'bg-green-700 hover:bg-green-600 text-white'
                    }`}>
                    {isMember(u.id) ? 'Remover' : 'Adicionar'}
                  </button>
                </div>
              ))}
            </div>
          )}

          {searchResults.length === 0 && search && !searching && (
            <p className="text-slate-500 text-sm text-center py-2">
              Nenhum usuário encontrado para "{search}"
            </p>
          )}
        </div>

        <button
          onClick={onClose}
          className="w-full mt-6 bg-slate-700 hover:bg-slate-600 text-white py-3 rounded-lg font-semibold transition-colors">
          Fechar
        </button>
      </div>
    </div>
  )
}
