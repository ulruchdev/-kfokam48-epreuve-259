import { createBrowserRouter } from 'react-router-dom'
import { Layout } from './components/Layout'
import { RequireRole } from './components/RequireRole'
import { IdentityScreen } from './features/identity/IdentityScreen'
import { TrainerHome } from './features/trainer/TrainerHome'
import { StudentHome } from './features/student/StudentHome'
import { ReviewerHome } from './features/reviewer/ReviewerHome'

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
