package staysplit.hotel_reservation.payment.webhook.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staysplit.hotel_reservation.payment.webhook.dto.PortOneWebhookRequest;
import staysplit.hotel_reservation.payment.webhook.service.PortOneWebhookFacade;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/portone")
public class PortOneWebHookController {

    private final PortOneWebhookFacade portOneWebhookFacade;

    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestHeader Map<String, String> headers,
                                              @RequestBody PortOneWebhookRequest request) {

        portOneWebhookFacade.verifyWebhookPayment(request);

        log.info("[포트원에 200 response 반환]");
        return ResponseEntity.ok().build();
    }

}
