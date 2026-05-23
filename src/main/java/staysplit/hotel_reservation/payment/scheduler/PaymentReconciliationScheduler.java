package staysplit.hotel_reservation.payment.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.payment.service.PaymentReconciliationService;

@Component
@RequiredArgsConstructor
public class PaymentReconciliationScheduler {

    private final PaymentReconciliationService paymentReconciliationService;

    @Scheduled(fixedDelay = 300_000) //5분 = 5 min * 60 sec * 1000 ms = 300,000
    public void performPaymentReconciliation() {
        paymentReconciliationService.processPaidInPGbutNotProcessedInServer();
        paymentReconciliationService.processRefundedOrFailedInPGbutPaidInServer();
    }
}

