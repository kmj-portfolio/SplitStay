package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import staysplit.hotel_reservation.payment.portone.client.PortOneClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCompensationService {

    private final PortOneClient portOneClient;

    public void cancelPaymentSilently(String paymentId, String reason) {
        try {
            portOneClient.cancelPayment(paymentId, reason);
        } catch (Exception e) {
            log.error("[중요] 포트원 결제 취소 실패. 수동 환불 필요. paymentId={}",paymentId);
        }
    }
}
