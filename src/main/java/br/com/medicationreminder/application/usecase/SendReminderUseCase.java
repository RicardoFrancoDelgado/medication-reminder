package br.com.medicationreminder.application.usecase;

import br.com.medicationreminder.application.gateway.MessageGateway;
import br.com.medicationreminder.domain.model.Medication;
import br.com.medicationreminder.domain.model.ReminderSession;
import br.com.medicationreminder.domain.model.ReminderStatus;
import br.com.medicationreminder.domain.repository.MedicationRepository;
import br.com.medicationreminder.domain.repository.ReminderSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendReminderUseCase {
    private final MedicationRepository medicationRepository;
    private final ReminderSessionRepository reminderSessionRepository;
    private final MessageGateway messageGateway;

    @Transactional
    public void execute(LocalTime start, LocalTime end) {
        List<Medication> medications = medicationRepository.findActiveMedicationScheduleBetween(start, end);

        if (medications.isEmpty()) {
            log.info("Nenhum medicamento encontrado para o intervalo {} - {} ", start, end);
            return;
        }

        for (Medication medication : medications) {
            ReminderSession session = buildSession(medication, start);
            reminderSessionRepository.save(session);

            String message = buildMessage(medication);
            messageGateway.sendMessage(
                    medication.getUser().getWhatsappNumber(), message
            );

            log.info("Lembrete enviado para {} - remédio: {}", medication.getUser().getName(), medication.getName());
        }
    }

    private ReminderSession buildSession(Medication medication, LocalTime scheduledTime) {
        return ReminderSession.builder()
                .medication(medication)
                .scheduleTime(scheduledTime)
                .sentAt(LocalDateTime.now())
                .status(ReminderStatus.PENDING)
                .retryCount(0).build();
    }

    private String buildMessage(Medication medication) {
        return String.format(
                "Olá, %s! Está na hora de tomar o %s. Você já tomou? Responda *sim* ou *não*.",
                medication.getUser().getName(),
                medication.getName()
        );
    }
}
