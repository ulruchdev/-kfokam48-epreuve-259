import axios, { AxiosError } from 'axios'
import { z } from 'zod'

/**
 * Every error body from the API is {code, message} (message in French — display it, F ENF4).
 */
export const errorBodySchema = z.object({
  code: z.string(),
  message: z.string(),
})

export class ApiError extends Error {
  readonly code: string
  readonly status: number | undefined

  constructor(code: string, message: string, status: number | undefined) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
  }
}

export const http = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (error instanceof AxiosError) {
      const parsed = errorBodySchema.safeParse(error.response?.data)
      if (parsed.success) {
        return Promise.reject(
          new ApiError(parsed.data.code, parsed.data.message, error.response?.status),
        )
      }
      return Promise.reject(
        new ApiError(
          'ERREUR_RESEAU',
          "Impossible de contacter le serveur. Vérifiez votre connexion.",
          error.response?.status,
        ),
      )
    }
    return Promise.reject(error)
  },
)
