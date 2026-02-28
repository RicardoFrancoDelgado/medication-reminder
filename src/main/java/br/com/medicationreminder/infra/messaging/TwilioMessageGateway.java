package br.com.medicationreminder.infra.messaging;

import br.com.medicationreminder.application.gateway.MessageGateway;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TwilioMessageGateway implements MessageGateway {
    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-number}")
    private String fromNumber;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio inicializado com sucesso.");
    }

    @Override
    public void sendMessage(String to, String message) {
        try {
            Message.creator(
                    new PhoneNumber("whatsapp:" + to),
                    new PhoneNumber("whatsapp:" + fromNumber),
                    message
            ).create();

            log.info("Mensagem enviada para {}", to);

        } catch (Exception e) {
            log.error("Erro ao enviar mensagem para {}: {}", to, e.getMessage());
        }
    }
}
