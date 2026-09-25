package com.kfokam48.attendance.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "presence")
public class Attendance {

    /** Imposed contract values (source): ETUDIANT, FORMATEUR. */
    public enum Source { ETUDIANT, FORMATEUR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "etudiant_id", nullable = false)
    private Long studentId;

    /** RG12 (Q14): ETUDIANT by self service, FORMATEUR when added by the trainer. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "marque_a", nullable = false)
    private OffsetDateTime markedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public OffsetDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(OffsetDateTime markedAt) { this.markedAt = markedAt; }
}
