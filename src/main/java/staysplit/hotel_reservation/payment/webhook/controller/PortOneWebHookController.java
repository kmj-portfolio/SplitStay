package staysplit.hotel_reservation.payment.webhook.controller;

import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.payment.webhook.dto.PortOneWebhookRequest;
import staysplit.hotel_reservation.payment.webhook.service.PortOneSignatureVerifier;
import staysplit.hotel_reservation.payment.webhook.service.PortOneWebhookFacade;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/portone")
public class PortOneWebHookController {

    private final PortOneSignatureVerifier signatureVerifier;
    private final PortOneWebhookFacade portOneWebhookFacade;
    private final PortOneSignatureVerifier portOneSignatureVerifier;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "webhook-id") String webhookId,
            @RequestHeader(value = "webhook-signautre") String webhookSignature,
            @RequestHeader(value = "webhook-timestamp") String webhookTimestamp) throws WebhookVerificationException {

        PortOneWebhookRequest request = signatureVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);

        portOneWebhookFacade.verifyWebhookPayment(request);

        log.info("[포트원에 200 response 반환]");
        return ResponseEntity.ok().build();
    }

}
