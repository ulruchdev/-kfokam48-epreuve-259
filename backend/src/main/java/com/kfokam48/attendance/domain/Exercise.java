package com.kfokam48.attendance.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exercice")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private String lien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseStatus status = ExerciseStatus.PENDING_ASSIGNMENT;

    @Column(name = "depose_a", nullable = false)
    private OffsetDateTime submittedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien =  lien; }
    public ExerciseStatus getStatus() { return status; }
    public void setStatus(ExerciseStatus status) { this.status = status; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; }
}
