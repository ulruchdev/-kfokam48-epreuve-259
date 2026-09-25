import { identityHandlers } from '../../features/identity/identity.handlers'
import { trainerHandlers } from '../../features/trainer/trainer.handlers'
import { studentHandlers } from '../../features/student/student.handlers'
import { reviewerHandlers } from '../../features/reviewer/reviewer.handlers'

/** Merge each feature's handlers here as they are built. */
export const handlers = [
  ...identityHandlers,
  ...trainerHandlers,
  ...studentHandlers,
  ...reviewerHandlers,
]
