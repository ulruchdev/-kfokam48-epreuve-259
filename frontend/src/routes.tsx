import { Navigate, createBrowserRouter } from 'react-router-dom'
import { Layout } from './components/Layout'
import { IdentityScreen } from './features/identity/IdentityScreen'
import { TrainerHome } from './features/trainer/TrainerHome'
import { StudentHome } from './features/student/StudentHome'
import { ReviewerHome } from './features/reviewer/ReviewerHome'
import { useIdentityStore, type Role } from './stores/identityStore'

function RequireRole({ role, children }: { role: Role; children: React.ReactNode }) {
  const identity = useIdentityStore((state) => state.identity)
  if (!identity || identity.role !== role) {
    return <Navigate to="/" replace />
  }
  return children
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <IdentityScreen /> },
      {
        path: 'formateur',
        element: (
          <RequireRole role="FORMATEUR">
            <TrainerHome />
          </RequireRole>
        ),
      },
      {
        path: 'etudiant',
        element: (
          <RequireRole role="ETUDIANT">
            <StudentHome />
          </RequireRole>
        ),
      },
      {
        path: 'relecteur',
        element: (
          <RequireRole role="RELECTEUR">
            <ReviewerHome />
          </RequireRole>
        ),
      },
    ],
  },
])
