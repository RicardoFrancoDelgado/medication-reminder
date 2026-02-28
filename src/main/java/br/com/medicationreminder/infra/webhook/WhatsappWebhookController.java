package br.com.medicationreminder.infra.webhook;

import br.com.medicationreminder.application.usecase.HandlerUserResponseUseCase;
import com.twilio.security.RequestValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook")
public class WhatsappWebhookController {
    private final HandlerUserResponseUseCase handlerUserResponseUseCase;

    @Value("${twilio.auth-token}")
    private String authToken;

    public ResponseEntity<Void> receiveMessage(@RequestParam Map<String, String> params, HttpServletRequest request) {
        if (!isValidTwilioRequest(request, params)) {
            log.warn("Requisição inválida recebida no webhook - possível tentativa de acesso indevido.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String from = params.get("From");
        String body = params.get("Body");

        if (from == null || body == null) {
            log.warn("Parâmetros inválidos recebidos no webhook: from={}, to={}", from, body);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        String whatsappNumber = from.replace("whatsapp", "");

        log.info("Mensagem recebida de {}: {}", whatsappNumber, body);
        handlerUserResponseUseCase.execute(whatsappNumber, body);
        return ResponseEntity.ok().build();
    }

    private boolean isValidTwilioRequest(HttpServletRequest request, Map<String, String> params) {
        try {
            RequestValidator validator = new RequestValidator(authToken);
            String requestUrl = request.getRequestURL().toString();
            Map<String, String> sortedParams = new HashMap<>(params);
            String twilioSignature = request.getHeader("X-Twilio-Signature");

            return validator.validate(requestUrl, sortedParams, twilioSignature);
        } catch (Exception e) {
            log.error("Erro ao validar assinatura do Twilio: {}", e.getMessage());
            return false;
        }
    }
}
