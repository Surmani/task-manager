import { useAuthStore } from '../../store/authStore'
import { useNavigate, useLocation } from 'react-router-dom'

export default function Navbar() {
  const { user, logout } = useAuthStore()
  const navigate = useNavigate()
  const location = useLocation()
  const isOnBoard = location.pathname.includes('/board')

  return (
    <nav className="bg-slate-900 text-white px-6 py-4 flex items-center justify-between shadow-lg">
      <div className="flex items-center gap-4">
        {isOnBoard && (
          <button
            onClick={() => navigate('/projects')}
            className="text-slate-400 hover:text-white transition-colors text-sm">
            ← Projetos
          </button>
        )}
        <span
          className="text-xl font-bold cursor-pointer"
          onClick={() => navigate('/projects')}>
          ⚡ Task Manager
        </span>
      </div>
      <div className="flex items-center gap-4">
        <span className="text-slate-300 text-sm">{user?.name}</span>
        <span className="text-xs bg-slate-700 px-2 py-1 rounded">{user?.role}</span>
        <button
          onClick={() => { logout(); navigate('/login') }}
          className="text-sm bg-red-600 hover:bg-red-700 px-3 py-1 rounded transition-colors">
          Sair
        </button>
      </div>
    </nav>
  )
}
