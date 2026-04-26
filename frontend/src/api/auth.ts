import api from './client'
import { User } from '../types'

export interface AuthResponse {
  token: string
  user: User
}

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/auth/login', { email, password }).then(r => r.data)

export const register = (name: string, email: string, password: string, role: string) =>
  api.post<AuthResponse>('/auth/register', { name, email, password, role }).then(r => r.data)
