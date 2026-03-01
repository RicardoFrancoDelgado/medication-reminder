package br.com.medicationreminder.domain.repository;

import br.com.medicationreminder.domain.model.ReminderSession;
import br.com.medicationreminder.domain.model.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderSessionRepository extends JpaRepository<ReminderSession, UUID> {
    List<ReminderSession> findByStatus(ReminderStatus status);

    @Query("""
        SELECT rs FROM ReminderSession rs
        WHERE rs.status = :status
        AND rs.sentAt <= :cutoff
    """)
    List<ReminderSession> findPendingSessionsOlderThan(
            @Param("status") ReminderStatus status,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("""
        SELECT rs FROM ReminderSession rs
        WHERE rs.medication.user.whatsappNumber = :whatsappNumber
        AND rs.status = 'PENDING'
        ORDER BY rs.sentAt DESC
        LIMIT 1
    """)
    Optional<ReminderSession> findLatestPendingSessionByWhatsappNumber(
            @Param("whatsappNumber") String whatsappNumber
    );

    @Query("""
        SELECT COUNT(rs) > 0 FROM ReminderSession rs
        WHERE rs.medication.id = :medicationId
        AND rs.scheduledTime = :scheduledTime
        AND rs.sentAt >= :startOfDay
        AND rs.sentAt < :endOfDay
    """)
    boolean existsSessionForToday(
            @Param("medicationId") UUID medicationId,
            @Param("scheduledTime") LocalTime scheduledTime,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
