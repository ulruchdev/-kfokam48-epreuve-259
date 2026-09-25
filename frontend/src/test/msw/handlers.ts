import { identityHandlers } from '../../features/identity/identity.handlers'
import { trainerHandlers } from '../../features/trainer/trainer.handlers'

/** Merge each feature's handlers here as they are built. */
export const handlers = [...identityHandlers, ...trainerHandlers]
