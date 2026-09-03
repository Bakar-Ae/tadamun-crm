import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { CommandMenu } from './CommandMenu'
import { searchWorkspace } from '../services/globalSearchService'
import {
  WorkspaceContext,
  type WorkspaceContextValue,
} from '../workspace/WorkspaceContextValue'

vi.mock('../services/globalSearchService', () => ({
  searchWorkspace: vi.fn(),
}))

const mockedSearchWorkspace = vi.mocked(searchWorkspace)

function LocationDisplay() {
  const location = useLocation()
  return <output data-testid="location">{location.pathname + location.search}</output>
}

function renderCommandMenu() {
  const workspaceContext: WorkspaceContextValue = {
    workspaces: [],
    activeWorkspace: {
      organizationId: 1,
      membershipId: 1,
      name: 'Tadamun',
      slug: 'tadamun',
      timeZone: 'Africa/Mogadishu',
      role: 'ADMIN',
      dataScope: 'ALL',
      permissions: [
        'DASHBOARD_VIEW',
        'CUSTOMER_VIEW',
        'LEAD_VIEW',
        'CONTACT_VIEW',
        'TASK_VIEW',
        'NOTE_VIEW',
      ],
    },
    status: 'ready',
    selectWorkspace: vi.fn(),
    reloadWorkspaces: vi.fn(),
  }

  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <WorkspaceContext.Provider value={workspaceContext}>
        <CommandMenu />
        <LocationDisplay />
      </WorkspaceContext.Provider>
    </MemoryRouter>,
  )
}

describe('CommandMenu', () => {
  beforeEach(() => {
    mockedSearchWorkspace.mockReset()
  })

  it('opens with Ctrl+K and waits for two search characters', async () => {
    renderCommandMenu()
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true })

    const input = screen.getByPlaceholderText(
      'Search customers, leads, contacts, tasks, and notes',
    )

    await userEvent.type(input, 't')

    expect(mockedSearchWorkspace).not.toHaveBeenCalled()
    expect(screen.getByText('Type at least two characters to search CRM records.')).toBeInTheDocument()
  })

  it('searches records and navigates to the selected detail route', async () => {
    mockedSearchWorkspace.mockResolvedValue({
      query: 'tadamun',
      results: [
        {
          module: 'CUSTOMER',
          id: 42,
          title: 'Tadamun Company',
          description: 'Tadamun Business Solutions',
          status: 'ACTIVE',
          parentModule: null,
          parentId: null,
        },
      ],
    })

    renderCommandMenu()
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true })

    await userEvent.type(
      screen.getByPlaceholderText(
        'Search customers, leads, contacts, tasks, and notes',
      ),
      'tadamun',
    )

    await waitFor(() => {
      expect(mockedSearchWorkspace).toHaveBeenCalledWith(
        'tadamun',
        null,
        expect.any(AbortSignal),
      )
    })

    await userEvent.click(await screen.findByText('Tadamun Company'))

    expect(screen.getByTestId('location')).toHaveTextContent('/customers?customerId=42')
  })

  it('shows a recoverable message when the search request fails', async () => {
    mockedSearchWorkspace.mockRejectedValue(new Error('Network unavailable'))

    renderCommandMenu()
    fireEvent.keyDown(window, { key: 'k', ctrlKey: true })

    await userEvent.type(
      screen.getByPlaceholderText(
        'Search customers, leads, contacts, tasks, and notes',
      ),
      'customer',
    )

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Search is unavailable. Please try again.',
    )
  })
})
