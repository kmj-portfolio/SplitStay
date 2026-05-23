package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.payment.portone.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.payment.portone.client.PortOneClient;
import staysplit.hotel_reservation.payment.portone.exception.PortOneException;
import staysplit.hotel_reservation.payment.portone.validator.PortOnePaymentValidator;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.service.ReservationValidator;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PortOneClient portOneClient;
    private final ReservationValidator reservationValidator;
    private final PaymentTransactionService paymentTransactionService;
    private final PortOnePaymentValidator portOnePaymentValidator;
    private final PaymentCompensationService paymentCompensationService;

    public void processPaidInPGbutNotProcessedInServer() {
        List<PaymentEntity> paymentEntityList = paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(
                List.of(PaymentStatus.READY, PaymentStatus.FAILED),
                LocalDateTime.now().minusMinutes(35));

        for (PaymentEntity paymentEntity : paymentEntityList) {
            String paymentId = paymentEntity.getPaymentId();
            try {
                PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(paymentId);
                if (!portOnePaymentResponse.status().equals("PAID")) continue;

                ReservationEntity reservation = paymentEntity.getReservationParticipant().getReservation();
                log.debug("[예약이 만료되었는지 확인] reservationId={})", reservation.getId());
                reservationValidator.validateNotExpired(reservation);

                log.debug("[결제 유효성 검증]");
                portOnePaymentValidator.validateForConfirmation(portOnePaymentResponse, paymentEntity);

                paymentTransactionService.processConfirmation(paymentId);

            //  이 오류를 구분하지 않으면 포트원 API 조회 중 네트워크 에러가 나면 멀쩡한 결제가 취소됨
            } catch (PortOneException e) {
                log.warn("[PortOne API 오류 - 다음 주기에 재시도] paymentId={}", paymentId);

            } catch (Exception e) {
                log.error("[결제 처리 중 에러 발생 - 자동 취소 진행] paymentId={}", paymentId, e);
                paymentCompensationService.cancelPaymentSilently(paymentId, "처리 실패로 자동 취소");
            }
        }
    }

    public void processRefundedOrFailedInPGbutPaidInServer() {
        List<PaymentEntity> paymentEntities = paymentRepository.findPaymentEntitiesByStatusInAndCreatedAtAfter(
                List.of(PaymentStatus.PAID),
                LocalDateTime.now().minusMinutes(60));

        for (PaymentEntity paymentEntity : paymentEntities) {
            String paymentId = paymentEntity.getPaymentId();
            try {
                PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(paymentId);
                if (!portOnePaymentResponse.status().equals("REFUNDED") && !portOnePaymentResponse.status().equals("FAILED")) continue;

                paymentTransactionService.processCancellation(paymentId);

            } catch (Exception e) {
                log.error("[결제 처리 중 에러 발생] paymentId={}", paymentId, e);
            }
        }
    }
}
