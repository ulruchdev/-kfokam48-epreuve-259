package com.kfokam48.attendance.web.erreur;

import org.springframework.http.HttpStatus;

/**
 * All business exceptions. Each maps to one imposed error code + status (B2).
 * Grouped in one file for readability; each class is tiny by design.
 */
public final class BusinessExceptions {

    private BusinessExceptions() {}

    public static class PromotionUnknownException extends ApiException {
        public PromotionUnknownException(Long id) {
            super("PROMOTION_INCONNUE", HttpStatus.NOT_FOUND, "La promotion " + id + " est inconnue.");
        }
    }

    public static class StudentUnknownException extends ApiException {
        public StudentUnknownException(Long id) {
            super("ETUDIANT_INCONNU", HttpStatus.NOT_FOUND, "L'étudiant " + id + " est inconnu.");
        }
    }

    /** EF7: the trainer can only add a student of the session's promotion. */
    public static class StudentNotInPromotionException extends ApiException {
        public StudentNotInPromotionException(Long id) {
            super("ETUDIANT_HORS_PROMOTION", HttpStatus.BAD_REQUEST,
                    "L'étudiant " + id + " n'appartient pas à la promotion de cette session.");
        }
    }

    public static class CodeUnknownException extends ApiException {
        public CodeUnknownException() {
            super("CODE_INCONNU", HttpStatus.BAD_REQUEST, "Le code de présence est inconnu.");
        }
    }

    /** RG3 (Q4): five wrong codes lock the student for two minutes — 429 Too Many Requests. */
    public static class TooManyAttemptsException extends ApiException {
        public TooManyAttemptsException() {
            super("TOO_MANY_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de tentatives : nouvel essai possible dans deux minutes.");
        }
    }

    public static class CodeExpiredException extends ApiException {
        public CodeExpiredException() {
            super("CODE_EXPIRE", HttpStatus.GONE, "Le code de présence a expiré.");
        }
    }

    public static class AlreadyPresentException extends ApiException {
        public AlreadyPresentException() {
            super("DEJA_PRESENT", HttpStatus.CONFLICT,
                    "La présence a déjà été enregistrée pour cette session.");
        }
    }

    public static class SessionUnknownException extends ApiException {
        public SessionUnknownException(Long id) {
            super("SESSION_INCONNUE", HttpStatus.NOT_FOUND, "La session " + id + " est inconnue.");
        }
    }

    public static class SessionClosedException extends ApiException {
        public SessionClosedException() {
            super("SESSION_CLOTUREE", HttpStatus.CONFLICT, "La session est clôturée : opération refusée.");
        }
    }

    /** DEC-2: the adjusted session end cannot precede the opening. */
    public static class EndBeforeOpeningException extends ApiException {
        public EndBeforeOpeningException() {
            super("FIN_AVANT_OUVERTURE", HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être postérieure à l'ouverture de la session.");
        }
    }

    public static class SessionAlreadyClosedException extends ApiException {
        public SessionAlreadyClosedException() {
            super("SESSION_DEJA_CLOTUREE", HttpStatus.CONFLICT, "La session est déjà clôturée.");
        }
    }

    /** RG14 (DEC-4): submitting an exercise requires prior attendance. */
    public static class AttendanceRequiredException extends ApiException {
        public AttendanceRequiredException() {
            super("PRESENCE_REQUISE", HttpStatus.BAD_REQUEST,
                    "Vous devez marquer votre présence avant de déposer un exercice.");
        }
    }

    public static class ExerciseAlreadySubmittedException extends ApiException {
        public ExerciseAlreadySubmittedException() {
            super("EXERCICE_DEJA_DEPOSE", HttpStatus.CONFLICT,
                    "Un exercice a déjà été déposé pour cette session.");
        }
    }

    public static class InvalidLinkException extends ApiException {
        public InvalidLinkException() {
            super("LIEN_INVALIDE", HttpStatus.BAD_REQUEST, "Le lien de l'exercice est invalide.");
        }
    }

    public static class ExerciseUnknownException extends ApiException {
        public ExerciseUnknownException(Long id) {
            super("EXERCICE_INCONNU", HttpStatus.NOT_FOUND, "L'exercice " + id + " est inconnu.");
        }
    }

    public static class ReviewUnknownException extends ApiException {
        public ReviewUnknownException(Long id) {
            super("RELECTURE_INCONNUE", HttpStatus.NOT_FOUND, "La relecture " + id + " est inconnue.");
        }
    }

    /** RG4 (Q5): a student can never review their own exercise. */
    public static class SelfReviewException extends ApiException {
        public SelfReviewException() {
            super("AUTO_RELECTURE", HttpStatus.FORBIDDEN,
                    "Un étudiant ne peut pas relire son propre exercice.");
        }
    }

    public static class ReviewNotAssignedException extends ApiException {
        public ReviewNotAssignedException() {
            super("RELECTURE_NON_ASSIGNEE", HttpStatus.FORBIDDEN,
                    "Seul le relecteur assigné peut agir sur cette relecture.");
        }
    }

    /** Imposed 409: POST on an already rendered review. */
    public static class ReviewAlreadyRenderedException extends ApiException {
        public ReviewAlreadyRenderedException() {
            super("RELECTURE_DEJA_RENDUE", HttpStatus.CONFLICT,
                    "Cette relecture a déjà été rendue.");
        }
    }

    /** RG8 (Q9): integer between 0 and 20. */
    public static class InvalidGradeException extends ApiException {
        public InvalidGradeException() {
            super("NOTE_INVALIDE", HttpStatus.BAD_REQUEST,
                    "La note doit être un entier entre 0 et 20.");
        }
    }

    /** RG11 (Q13): the link is replaceable until a review has started. */
    public static class ReviewStartedException extends ApiException {
        public ReviewStartedException() {
            super("RELECTURE_COMMENCEE", HttpStatus.CONFLICT,
                    "La relecture a commencé : le lien ne peut plus être remplacé.");
        }
    }
}
