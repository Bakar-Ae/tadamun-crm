import type { GlobalSearchResult } from '../services/globalSearchService'

export function getGlobalSearchResultPath(result: GlobalSearchResult): string {
  switch (result.module) {
    case 'CUSTOMER':
      return `/customers?customerId=${result.id}`
    case 'LEAD':
      return `/leads?leadId=${result.id}`
    case 'CONTACT':
      return `/contacts?contactId=${result.id}`
    case 'TASK':
      return `/tasks?taskId=${result.id}`
    case 'NOTE':
      if (result.parentModule === 'CUSTOMER' && result.parentId !== null) {
        return `/customers?customerId=${result.parentId}`
      }

      if (result.parentModule === 'LEAD' && result.parentId !== null) {
        return `/leads?leadId=${result.parentId}`
      }

      return '/notes'
  }
}
