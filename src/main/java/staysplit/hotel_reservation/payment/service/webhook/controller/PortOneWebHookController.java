package staysplit.hotel_reservation.payment.service.webhook.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/webhooks/portone")
public class PortOneWebHookController {
    
    @PostMapping
    public ResponseEntity<Void> handleWebhook(@RequestHeader Map<String, String> headers,
                                              @RequestBody String rawBody) {

        log.info("headers: {}", headers);
        for (String key : headers.keySet()) {
            log.info(key + ": " + headers.get(key));
        }
        log.info("rawBody: {}", rawBody);
        return ResponseEntity.ok().build();
    }

}
