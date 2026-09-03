import { lazy, Suspense, type ReactNode } from "react";
import type { PermissionName } from './services/permissionService'
import {
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router";
import { RouteErrorBoundary } from './components/RouteErrorBoundary'
import { WorkspaceAccessGate } from './components/WorkspaceAccessGate'
import { useWorkspace } from './workspace/useWorkspace'

const LoginPage = lazy(() =>
  import("./pages/LoginPage").then((module) => ({ default: module.LoginPage })),
);
const DashboardPage = lazy(() =>
  import("./pages/DashboardPage").then((module) => ({
    default: module.DashboardPage,
  })),
);
const UsersPage = lazy(() =>
  import("./pages/UsersPage").then((module) => ({ default: module.UsersPage })),
);
const CustomersPage = lazy(() =>
  import("./pages/CustomersPage").then((module) => ({
    default: module.CustomersPage,
  })),
);
const LeadsPage = lazy(() =>
  import("./pages/LeadsPage").then((module) => ({ default: module.LeadsPage })),
);
const ContactsPage = lazy(() =>
  import("./pages/ContactsPage").then((module) => ({
    default: module.ContactsPage,
  })),
);
const TasksPage = lazy(() =>
  import("./pages/TasksPage").then((module) => ({ default: module.TasksPage })),
);
const NotesPage = lazy(() =>
  import("./pages/NotesPage").then((module) => ({ default: module.NotesPage })),
);
const ReportsPage = lazy(() =>
  import("./pages/ReportsPage").then((module) => ({
    default: module.ReportsPage,
  })),
);
const AuditLogsPage = lazy(() =>
  import("./pages/AuditLogsPage").then((module) => ({
    default: module.AuditLogsPage,
  })),
);
const NotificationsPage = lazy(() =>
  import("./pages/NotificationsPage").then((module) => ({
    default: module.NotificationsPage,
  })),
);
const ChangePasswordPage = lazy(() =>
  import("./pages/ChangePasswordPage").then((module) => ({
    default: module.ChangePasswordPage,
  })),
);
const ForgotPasswordPage = lazy(() =>
  import("./pages/ForgotPasswordPage").then((module) => ({
    default: module.ForgotPasswordPage,
  })),
);
const ResetPasswordPage = lazy(() =>
  import("./pages/ResetPasswordPage").then((module) => ({
    default: module.ResetPasswordPage,
  })),
);
const RolePermissionsPage = lazy(() =>
  import('./pages/RolePermissionsPage').then((module) => ({
    default: module.RolePermissionsPage,
  })),
);
const CalendarPage = lazy(() =>
  import('./pages/CalendarPage').then((module) => ({
    default: module.CalendarPage,
  })),
)
const AcceptInvitationPage = lazy(() =>
  import('./pages/AcceptInvitationPage').then((module) => ({
    default: module.AcceptInvitationPage,
  })),
)

function PageLoader() {
  return (
    <div className="grid min-h-screen place-items-center bg-[var(--crm-bg)] text-[var(--crm-text)]">
      <div className="rounded-2xl border border-[var(--crm-border)] bg-[var(--crm-surface-glass)] px-5 py-4 text-sm shadow-[var(--crm-shadow-soft)]">
        Loading Tadamun...
      </div>
    </div>
  );
}

function getStoredUser() {
  const storedUser = localStorage.getItem("user");

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser) as {
     passwordChangeRequired?: boolean
     permissions?: PermissionName[]
  }
  } catch {
    localStorage.removeItem("user");
    return null;
  }
}

function PublicOnly({ children }: { children: ReactNode }) {
  const token = localStorage.getItem("token");

  if (token) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

function ProtectedRoute({
  children,
  requiredPermission,
  requiresWorkspace = true,
}: {
  children: ReactNode;
  requiredPermission?: PermissionName;
  requiresWorkspace?: boolean;
}) {
  const location = useLocation();
  const token = localStorage.getItem("token");
  const user = getStoredUser();
  const passwordChangeRequired = user?.passwordChangeRequired === true;
  const { activeWorkspace, status } = useWorkspace()

  if (!token) {
    return <Navigate to="/" replace state={{ from: location.pathname }} />;
  }

  if (passwordChangeRequired && location.pathname !== "/change-password") {
    return <Navigate to="/change-password" replace />;
  }

  if (requiresWorkspace && status === 'loading') {
    return <PageLoader />
  }

  if (requiresWorkspace && (status === 'error' || !activeWorkspace)) {
    return <WorkspaceAccessGate />
  }

  if (
    requiredPermission &&
    requiresWorkspace &&
    !activeWorkspace?.permissions.includes(requiredPermission)
  ) {
    return <Navigate to="/dashboard" replace />;
  }

  return children;
}

function App() {
  return (
    <RouteErrorBoundary>
      <Suspense fallback={<PageLoader />}>
        <Routes>
        <Route
          path="/"
          element={
            <PublicOnly>
              <LoginPage />
            </PublicOnly>
          }
        />
        <Route
          path="/login"
          element={
            <PublicOnly>
              <LoginPage />
            </PublicOnly>
          }
        />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />

        <Route
          path="/dashboard"
          element={
            <ProtectedRoute requiredPermission="DASHBOARD_VIEW">
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/users"
          element={
            <ProtectedRoute requiredPermission="USER_VIEW">
              <UsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/roles-permissions"
          element={
            <ProtectedRoute requiredPermission="PERMISSION_MANAGE">
              <RolePermissionsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customers"
          element={
            <ProtectedRoute requiredPermission="CUSTOMER_VIEW">
              <CustomersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/leads"
          element={
            <ProtectedRoute requiredPermission="LEAD_VIEW">
              <LeadsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/contacts"
          element={
            <ProtectedRoute requiredPermission="CONTACT_VIEW">
              <ContactsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/tasks"
          element={
            <ProtectedRoute requiredPermission="TASK_VIEW">
              <TasksPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/calendar"
          element={
            <ProtectedRoute requiredPermission="TASK_VIEW">
              <CalendarPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/notes"
          element={
            <ProtectedRoute requiredPermission="NOTE_VIEW">
              <NotesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/reports"
          element={
            <ProtectedRoute requiredPermission="REPORT_VIEW">
              <ReportsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/audit-logs"
          element={
            <ProtectedRoute requiredPermission="AUDIT_LOG_VIEW">
              <AuditLogsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/notifications"
          element={
            <ProtectedRoute>
              <NotificationsPage />
            </ProtectedRoute>
          }
        />
        <Route
         path="/accept-invitation"
         element={<AcceptInvitationPage />}
       />
        <Route
          path="/change-password"
          element={
            <ProtectedRoute requiresWorkspace={false}>
              <ChangePasswordPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </RouteErrorBoundary>
  );
}

export default App;
