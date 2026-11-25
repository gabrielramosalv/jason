import { publicEnv } from './runtime-env'

export const API_CONTEXT = publicEnv('VITE_API_BASE_URL') ?? '/api'
