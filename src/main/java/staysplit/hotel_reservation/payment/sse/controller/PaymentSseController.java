package staysplit.hotel_reservation.payment.sse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import staysplit.hotel_reservation.payment.sse.service.PaymentSseService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentSseController {

    private final PaymentSseService paymentSseService;

    @GetMapping(value = "/{paymentId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String paymentId, Authentication authentication) {
        return paymentSseService.connect(paymentId, authentication.getName());
    }
}
