package com.kfokam48.attendance.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Issue #62 — the V4 migration: existing data survive it (DEC-14) and the database itself
 * refuses a third review for an exercise (RG5 revised).
 */
class TwoReviewersMigrationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void should_keepTheSeededReviews_withRankOne_andSendSingleReviewerExercisesBackToAssignment_DEC14() {
        List<String> statuses = jdbc.queryForList("""
                SELECT x.statut FROM exercice x JOIN session s ON s.id = x.session_id
                WHERE s.code = 'INTRO1' AND s.statut = 'CLOTUREE'""", String.class);
        Integer seededReviews = jdbc.queryForObject("""
                SELECT count(*) FROM relecture r JOIN exercice x ON x.id = r.exercice_id
                JOIN session s ON s.id = x.session_id
                WHERE s.code = 'INTRO1' AND s.statut = 'CLOTUREE' AND r.rang = 1""", Integer.class);

        assertThat(statuses).containsOnly("EN_ATTENTE_AFFECTATION").hasSize(3);
        assertThat(seededReviews).isEqualTo(3);
    }

    @Test
    void should_refuseAThirdReview_atTheDatabaseLevel_RG5() {
        Long exerciseId = jdbc.queryForObject("""
                SELECT x.id FROM exercice x JOIN session s ON s.id = x.session_id JOIN etudiant e ON e.id = x.etudiant_id
                WHERE s.code = 'INTRO1' AND e.nom = 'Abanda Meli'""", Long.class);
        jdbc.update("INSERT INTO relecture (exercice_id, relecteur_id, rang) SELECT ?, id, 2 FROM etudiant WHERE nom = 'Eyenga Grace'", exerciseId);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO relecture (exercice_id, relecteur_id, rang) SELECT ?, id, 3 FROM etudiant WHERE nom = 'Kamga Yannick'", exerciseId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO relecture (exercice_id, relecteur_id, rang) SELECT ?, id, 2 FROM etudiant WHERE nom = 'Kamga Yannick'", exerciseId))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("DELETE FROM relecture WHERE exercice_id = ? AND rang = 2", exerciseId);
    }
}
