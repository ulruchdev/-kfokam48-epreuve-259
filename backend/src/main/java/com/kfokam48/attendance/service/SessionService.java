package com.kfokam48.attendance.service;

import com.kfokam48.attendance.domain.CourseSession;
import com.kfokam48.attendance.repository.CourseSessionRepository;
import com.kfokam48.attendance.repository.PromotionRepository;
import com.kfokam48.attendance.web.dto.Dto;
import com.kfokam48.attendance.web.erreur.BusinessExceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class SessionService {

    /** No ambiguous characters (no 0/O, 1/L/I) — ENF3. */
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int CODE_GENERATION_MAX_TRIES = 50;

    private final CourseSessionRepository sessions;
    private final PromotionRepository promotions;

    public SessionService(CourseSessionRepository sessions, PromotionRepository promotions) {
        this.sessions = sessions;
        this.promotions = promotions;
    }

    /** EF1 / RG1 (Q2) / DEC-2 / DEC-5 — the imposed POST /api/sessions. */
    @Transactional
    public Dto.SessionOpenedResponse open(Dto.OpenSessionRequest request) {
        requirePromotionExists(request.promotionId());
        int duration = request.effectiveDuration();
        OffsetDateTime now = OffsetDateTime.now();

        CourseSession session = new CourseSession();
        session.setPromotionId(request.promotionId());
        session.setTitre(request.titre().trim());
        session.setOpenedAt(now);
        session.setExpiresAt(now.plusMinutes(15));            // RG1
        session.setEndsAt(now.plusMinutes(duration));         // DEC-2
        session.setStatus(CourseSession.Status.OUVERTE);
        session.setCode(generateUniqueCode());                // DEC-5

        session = sessions.save(session);
        return toOpenedResponse(session);
    }

    public List<Dto.SessionSummaryResponse> listByPromotion(Long promotionId) {
        requirePromotionExists(promotionId);
        return sessions.findByPromotionIdOrderByOpenedAtDesc(promotionId).stream()
                .map(this::toSummary)
                .toList();
    }

    public Dto.SessionDetailResponse getDetail(Long id) {
        return toDetail(loadSession(id));
    }

    /** DEC-2: the trainer may adjust the session end time. */
    @Transactional
    public Dto.SessionDetailResponse adjustEnd(Long id, Dto.AdjustEndTimeRequest request) {
        CourseSession session = loadSession(id);
        requireSessionNotClosed(session);
        requireEndAfterOpening(session, request.endTime());
        session.setEndsAt(request.endTime());
        return toDetail(sessions.save(session));
    }

    /** EF8 / RG9 / RG10: freezes submissions, attendance and review amendments. */
    @Transactional
    public void close(Long id) {
        CourseSession session = loadSession(id);
        if (session.getStatus() == CourseSession.Status.CLOTUREE) {
            throw new SessionAlreadyClosedException();
        }
        session.setStatus(CourseSession.Status.CLOTUREE);
        sessions.save(session);
    }

    // ---------- private steps ----------

    private void requirePromotionExists(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw new PromotionUnknownException(promotionId);
        }
    }

    private void requireSessionNotClosed(CourseSession session) {
        if (session.getStatus() == CourseSession.Status.CLOTUREE) {
            throw new SessionClosedException();
        }
    }

    private void requireEndAfterOpening(CourseSession session, OffsetDateTime newEnd) {
        if (newEnd.isBefore(session.getOpenedAt())) {
            throw new EndBeforeOpeningException();
        }
    }

    /** DEC-5: unique among non-closed sessions. */
    private String generateUniqueCode() {
        SecureRandom random = new SecureRandom();
        for (int attempt = 0; attempt < CODE_GENERATION_MAX_TRIES; attempt++) {
            String code = randomCode(random);
            if (!sessions.existsByCodeAndStatusNot(code, CourseSession.Status.CLOTUREE)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique code");
    }

    private String randomCode(SecureRandom random) {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private CourseSession loadSession(Long id) {
        return sessions.findById(id).orElseThrow(() -> new SessionUnknownException(id));
    }

    private Dto.SessionOpenedResponse toOpenedResponse(CourseSession s) {
        return new Dto.SessionOpenedResponse(s.getId(), s.getCode(), s.getOpenedAt(),
                s.getExpiresAt(), s.getEndsAt(), s.getStatus().name());
    }

    private Dto.SessionSummaryResponse toSummary(CourseSession s) {
        return new Dto.SessionSummaryResponse(s.getId(), s.getTitre(), s.getOpenedAt(),
                s.getExpiresAt(), s.getEndsAt(), s.getStatus().name());
    }

    private Dto.SessionDetailResponse toDetail(CourseSession s) {
        return new Dto.SessionDetailResponse(s.getId(), s.getPromotionId(), s.getTitre(), s.getCode(),
                s.getOpenedAt(), s.getExpiresAt(), s.getEndsAt(), s.getStatus().name());
    }
}
