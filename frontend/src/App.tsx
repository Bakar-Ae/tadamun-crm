import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { UsersPage } from './pages/UsersPage'
import { CustomersPage } from './pages/CustomersPage'
import { LeadsPage } from './pages/LeadsPage'
import { ContactsPage } from './pages/ContactsPage'
import { TasksPage } from './pages/TasksPage'
import { AuditLogsPage } from './pages/AuditLogsPage'
import { NotesPage } from './pages/NotesPage'
import { ReportsPage } from './pages/ReportsPage'
import { ChangePasswordPage } from './pages/ChangePasswordPage'



function App() {
  const path = window.location.pathname
  const token = localStorage.getItem('token')
  const storedUser = localStorage.getItem('user')
  const user = storedUser ? JSON.parse(storedUser) : null
  const passwordChangeRequired = user?.passwordChangeRequired === true

  if (!token) {
    return <LoginPage />
  }
  if (passwordChangeRequired && path !== '/change-password') {
  setTimeout(() => {
    window.location.assign('/change-password')
  }, 0)

  return null
}
  if (path === '/users') {
    return <UsersPage />
  }
  if (path === '/contacts') {
  return <ContactsPage />
}

if (path === '/notes') {
  return <NotesPage />
}

if (path === '/reports') {
  return <ReportsPage />
}

if (path === '/audit-logs') {
  return <AuditLogsPage />
}

if (path === '/tasks') {
  return <TasksPage />
}
  if (path === '/customers') {
  return <CustomersPage />
}
if (path === '/leads') {
  return <LeadsPage />
}
if (path === '/change-password') {
  return <ChangePasswordPage />
}

  if (path === '/dashboard') {
    return <DashboardPage />
  }

  return <DashboardPage />
}


export default App