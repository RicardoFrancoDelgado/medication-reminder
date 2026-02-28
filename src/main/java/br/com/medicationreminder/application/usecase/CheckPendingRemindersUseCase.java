package br.com.medicationreminder.application.usecase;

import br.com.medicationreminder.application.gateway.MessageGateway;
import br.com.medicationreminder.domain.model.ReminderSession;
import br.com.medicationreminder.domain.model.ReminderStatus;
import br.com.medicationreminder.domain.repository.ReminderSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckPendingRemindersUseCase {

    private static final int MAX_RETRIES = 2;
    private static final int MINUTES_BEFORE_RETRY = 30;

    private final ReminderSessionRepository reminderSessionRepository;
    private final MessageGateway messageGateway;

    @Transactional
    public void execute() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(MINUTES_BEFORE_RETRY);

        List<ReminderSession> pendingSessions = reminderSessionRepository
                .findPendingSessionsOlderThan(ReminderStatus.PENDING, cutoff);

        for (ReminderSession session : pendingSessions) {
            if (session.getRetryCount() >= MAX_RETRIES) {
                expireSession(session);
            } else {
                retrySession(session);
            }
        }
    }

    private void retrySession(ReminderSession session) {
        session.setRetryCount(session.getRetryCount() + 1);
        session.setSentAt(LocalDateTime.now());
        reminderSessionRepository.save(session);

        String whatsappNumber = session.getMedication().getUser().getWhatsappNumber();
        String medicationName = session.getMedication().getName();
        String userName = session.getMedication().getUser().getName();

        messageGateway.sendMessage(
                whatsappNumber,
                String.format(
                        "Oi %s, lembrete %d/%d: você tomou o %s? Responda *sim* ou *não*.",
                        userName,
                        session.getRetryCount(),
                        MAX_RETRIES,
                        medicationName
                )
        );

        log.info("Reenvio {} de {} para sessão {}", session.getRetryCount(), MAX_RETRIES, session.getId());
    }

    private void expireSession(ReminderSession session) {
        session.setStatus(ReminderStatus.EXPIRED);
        reminderSessionRepository.save(session);

        log.warn("Sessão {} expirada sem resposta para o remédio {}",
                session.getId(),
                session.getMedication().getName()
        );
    }

}
