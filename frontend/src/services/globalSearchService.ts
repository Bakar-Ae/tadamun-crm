import { api } from './api'

export type SearchModule =
  | 'CUSTOMER'
  | 'LEAD'
  | 'CONTACT'
  | 'TASK'
  | 'NOTE'

export type GlobalSearchResult = {
  module: SearchModule
  id: number
  title: string
  description: string | null
  status: string | null
  parentModule: SearchModule | null
  parentId: number | null
}

export type GlobalSearchResponse = {
  query: string
  results: GlobalSearchResult[]
}

export async function searchWorkspace(
  query: string,
  module: SearchModule | null,
  signal?: AbortSignal,
) {
  const response = await api.get<GlobalSearchResponse>('/search', {
    params: {
      q: query,
      limitPerModule: 5,
      modules: module ?? undefined,
    },
    signal,
  })

  return response.data
}
