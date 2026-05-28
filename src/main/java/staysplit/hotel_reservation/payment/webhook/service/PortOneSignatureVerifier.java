package staysplit.hotel_reservation.payment.webhook.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.portone.sdk.server.webhook.WebhookVerifier;
import io.portone.sdk.server.errors.WebhookVerificationException;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.webhook.dto.PortOneWebhookRequest;


@Component
public class PortOneSignatureVerifier {

    private final WebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    public PortOneSignatureVerifier(@Value("${portone.webhook.secret}") String webhookSecret) {
        this.webhookVerifier = new WebhookVerifier(webhookSecret);
        this.objectMapper = new ObjectMapper();
    }

    public PortOneWebhookRequest verify(String rawBody, String webhookId, String webhookSignature, String webhookTimestamp) throws WebhookVerificationException {

        try {
            // SDK로 검증
            webhookVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);

            // Http Requets Body만 PortOneWebhookRequest로 parse
            return parseRequest(rawBody);

        } catch (WebhookVerificationException e) {
            throw new AppException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }

    private PortOneWebhookRequest parseRequest(String rawBody)  {
        try {
            return objectMapper.readValue(rawBody, PortOneWebhookRequest.class);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INVALID_WEBHOOK_SIGNATURE);
        }
    }
}
