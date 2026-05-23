package staysplit.hotel_reservation.payment.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.payment.portone.dto.response.PortOnePaymentResponse;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.portone.client.PortOneClient;
import staysplit.hotel_reservation.payment.portone.validator.PortOnePaymentValidator;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.payment.service.*;
import staysplit.hotel_reservation.payment.sse.service.PaymentSseService;
import staysplit.hotel_reservation.payment.webhook.dto.PortOneWebhookRequest;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.service.ReservationValidator;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneWebhookFacade {
    private final PortOneClient portOneClient;
    private final PaymentTransactionService paymentTransactionService;
    private final PortOnePaymentValidator portOnePaymentValidator;
    private final PaymentCompensationService paymentCompensationService;
    private final PaymentRepository paymentRepository;
    private final ReservationValidator reservationValidator;
    private final PaymentSseService paymentSseService;

    // Portone을 사용한 검증 조회
    public void verifyWebhookPayment(PortOneWebhookRequest request) {
        String paymentId = request.data().paymentId();
        log.info("[PortOneWebhook 수신] paymentId={}, type={}", paymentId, request.type());

        // 이미 검증이 완료되었는지 확인
        PaymentEntity paymentEntity = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    log.warn("[Portone Webhook에서 조회 불가능한 paymentId가 들어왔습니다.] paymentId={}", paymentId);
                    return new AppException(ErrorCode.PAYMENT_NOT_FOUND);
                });

        // 멱등성 보장: 이미 결제완료되었거나 취소처리된 경우 무시하고 200 응답
        if (paymentEntity.isPaid()) {
            log.info("[결제 검증 - 이미 결제 완료된 건, 멱등성 처리] paymentId={}", paymentId);
            return;
        }

        if (paymentEntity.isCancelled()) {
            log.warn("[결제 검증 - 이미 취소된 결제에 대한 재호출, 멱등성 처리] paymentId={}", paymentId);
            return;
        }

        // webhook: 결제 완료, server: 결제 미완료
        if (request.type().equals("Transaction.Paid")) {

            // PortOne에서 paymentId로 결제 내역 조회
            log.info("[포트원 결제 내역 조회] portOnePaymentId={}", paymentId);
            PortOnePaymentResponse portOnePaymentResponse = portOneClient.getPayment(paymentId);

            // 유효성 검증 후 DB 저장 시도
            try {
                // 예약이 만료되었으면 fail first
                ReservationEntity reservation = paymentEntity.getReservationParticipant().getReservation();
                log.debug("[예약이 만료되었는지 확인] reservationId={})", reservation.getId());
                reservationValidator.validateNotExpired(reservation);

                // 결제 유효성 검증
                log.debug("[결제 유효성 검증]");
                portOnePaymentValidator.validateForConfirmation(portOnePaymentResponse, paymentEntity);

                // Transaction내에서 PaymentEntity 생성 및 저장
                paymentTransactionService.processConfirmation(paymentId);

                paymentSseService.sendPaymentCompleted(paymentId);

            } catch (Exception e) {
                log.error("[결제 처리 중 에러 발생 - 자동 취소 진행] paymentId={}", paymentId, e);
                paymentCompensationService.cancelPaymentSilently(paymentId, "처리 실패로 자동 취소");
                paymentSseService.sendPaymentFailed(paymentId);
            }
        }
    }
}
