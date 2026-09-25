package com.kfokam48.attendance.domain;

/**
 * D4 state machine: exercise lifecycle.
 * {@code code} is the value stored in EXERCICE.statut and exposed by the API contract.
 */
public enum ExerciseStatus {
    PENDING_ASSIGNMENT("EN_ATTENTE_AFFECTATION"),   // submitted, no eligible reviewer yet (RG15)
    PENDING_REVIEW("EN_ATTENTE_RELECTURE"),         // reviewer assigned, review not rendered
    REVIEWED("RELU");                               // review rendered

    private final String code;

    ExerciseStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static ExerciseStatus fromCode(String code) {
        for (ExerciseStatus status : values()) {
            if (status.code.equals(code)) return status;
        }
        throw new IllegalArgumentException("Unknown exercise status: " + code);
    }
}
