package com.kfokam48.attendance.domain;

/** D4 state machine: exercise lifecycle. */
public enum ExerciseStatus {
    PENDING_ASSIGNMENT,   // submitted, no eligible reviewer yet (RG15)
    PENDING_REVIEW,       // reviewer assigned, review not rendered
    REVIEWED              // review rendered
}
