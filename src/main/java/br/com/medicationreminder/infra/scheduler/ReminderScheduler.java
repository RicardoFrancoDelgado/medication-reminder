package br.com.medicationreminder.infra.scheduler;

import br.com.medicationreminder.application.usecase.CheckPendingRemindersUseCase;
import br.com.medicationreminder.application.usecase.SendReminderUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final SendReminderUseCase sendReminderUseCase;
    private final CheckPendingRemindersUseCase checkPendingRemindersUseCase;

    @Scheduled(cron = "0 * * * * *")
    public void checkMedicationSchedules() {
        LocalTime now = LocalTime.now();
        LocalTime start = now.minusMinutes(1);
        LocalTime end = now.plusMinutes(1);

        log.info("Verificando medicamentos para o intervalo {} - {}", start, end);
        sendReminderUseCase.execute(start, end);
    }

    @Scheduled(fixedRate = 1800000)
    public void checkPendingReminders() {
        log.info("Verificando sessões pendentes sem resposta...");
        checkPendingRemindersUseCase.execute();
    }
}

