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

@Slf4j
@Service
@RequiredArgsConstructor
public class HandlerUserResponseUseCase {

    private final ReminderSessionRepository reminderSessionRepository;
    private final MessageGateway messageGateway;

    @Transactional
    public void execute(String whatsappNumber, String response) {
        ReminderSession session = reminderSessionRepository.findLatestPendingSessionByWhatsappNumber(whatsappNumber)
                .orElse(null);

        if (session == null) {
            log.warn("Nenhuma sessão encontrada para o número {}", whatsappNumber);
            return;
        }

        String normalizedResponse = response.trim().toLowerCase();

        if (normalizedResponse.equals("sim")) {
            session.setStatus(ReminderStatus.CONFIRMED);
            session.setRespondedAt(LocalDateTime.now());
            reminderSessionRepository.save(session);

            messageGateway.sendMessage(
                    whatsappNumber, "Ótimo! um tijolinho por dia. Continue assim! 💊"
            );

            log.info("Sessão {} confirmada para o número {}", session.getId(), whatsappNumber);
        } else if (normalizedResponse.equals("nao")) {
            session.setStatus(ReminderStatus.DENIED);
            session.setRespondedAt(LocalDateTime.now());
            reminderSessionRepository.save(session);

            messageGateway.sendMessage(
                    whatsappNumber,
                    "Aoba vamo lembrar de tomar esse remédio em! 🙏"
            );

            log.info("Sessão {} negada para o número {}", session.getId(), whatsappNumber);

        } else {
            messageGateway.sendMessage(
                    whatsappNumber,
                    "Não entendi sua resposta. Por favor, responda apenas *sim* ou *não*."
            );

            log.warn("Resposta inválida recebida do número {}: {}", whatsappNumber, response);
        }
    }
}
