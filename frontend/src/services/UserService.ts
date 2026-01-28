import { client } from '@/api/client'
import type { components } from '@/types/schema'
import { getErrorMessage } from '@/lib/utils'

const throwWithCode = (error: any) => {
    const msg = getErrorMessage(error)
    const err: any = new Error(msg)
    if (typeof error === 'object') {
        err.errorCode = error.errorCode
        err.status = error.status
    }
    throw err
}

export const UserService = {
  async getAll() {
    const { data, error } = await client.GET("/api/users", {})
    if (error) throwWithCode(error)
    return ((data as unknown) || []) as components['schemas']['MUser'][]
  },

  async getById(uuid: string) {
    const { data, error } = await client.GET("/api/users/{id}", { 
        params: { path: { id: uuid } } 
    })
    if (error) throwWithCode(error)
    return (data as unknown) as components['schemas']['MUser']
  },

  async create(payload: any) {
    const { error } = await client.POST('/api/users', { body: payload })
    if (error) throwWithCode(error)
  },

  async update(uuid: string, payload: any) {
    const { error } = await client.PUT('/api/users/{id}', {
        params: { path: { id: uuid } },
        body: payload
    })
    if (error) throwWithCode(error)
  },

  async delete(uuid: string) {
    const { error } = await client.DELETE('/api/users/{id}', { params: { path: { id: uuid } } })
    if (error) throwWithCode(error)
  }
}