package staysplit.hotel_reservation.payment.sse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentSseHeartbeatScheduler {

    private final PaymentSseService paymentSseService;

    @Scheduled(fixedRate = 30_000)
    public void sendHeartBeat() {
        paymentSseService.sendHeartBeatToAll();
    }
}
