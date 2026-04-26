import api from './client'
import { User } from '../types'

export const getUsers = () =>
  api.get<User[]>('/users').then(r => r.data)

export const searchUsers = (q: string) =>
  api.get<User[]>(`/users/search?q=${encodeURIComponent(q)}`).then(r => r.data)
