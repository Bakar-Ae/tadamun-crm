import { describe, expect, it } from 'vitest'
import { getGlobalSearchResultPath } from './globalSearch'
import type { GlobalSearchResult } from '../services/globalSearchService'

function result(
  module: GlobalSearchResult['module'],
  overrides: Partial<GlobalSearchResult> = {},
): GlobalSearchResult {
  return {
    module,
    id: 42,
    title: 'Test result',
    description: null,
    status: null,
    parentModule: null,
    parentId: null,
    ...overrides,
  }
}

describe('getGlobalSearchResultPath', () => {
  it.each([
    ['CUSTOMER', '/customers?customerId=42'],
    ['LEAD', '/leads?leadId=42'],
    ['CONTACT', '/contacts?contactId=42'],
    ['TASK', '/tasks?taskId=42'],
  ] as const)('maps a %s result to its detail route', (module, expectedPath) => {
    expect(getGlobalSearchResultPath(result(module))).toBe(expectedPath)
  })

  it('opens a customer note in its parent customer drawer', () => {
    expect(
      getGlobalSearchResultPath(
        result('NOTE', { parentModule: 'CUSTOMER', parentId: 7 }),
      ),
    ).toBe('/customers?customerId=7')
  })

  it('opens a lead note in its parent lead drawer', () => {
    expect(
      getGlobalSearchResultPath(
        result('NOTE', { parentModule: 'LEAD', parentId: 9 }),
      ),
    ).toBe('/leads?leadId=9')
  })

  it('falls back to the notes page when a note has no parent', () => {
    expect(getGlobalSearchResultPath(result('NOTE'))).toBe('/notes')
  })
})
