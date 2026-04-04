package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentValidator {

    private final PaymentRepository paymentRepository;

    public PaymentEntity validatePaymentByPaymentId(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    log.warn("[결제 내역을 찾지 못했습니다.] paymentId={}", paymentId);
                    return new AppException(ErrorCode.PAYMENT_NOT_FOUND);
                });
    }

    public void validatePaymentOwner(PaymentEntity payment, CustomerEntity customer) {
        if (!payment.getReservationParticipant().getCustomer().getId().equals(customer.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER);
        }
    }

    public void validateCancelable(PaymentEntity paymentEntity) {
        if (paymentEntity.isCancelled()) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }

        // 이미 숙박을 했는지 확인
        ReservationEntity reservation = paymentEntity.getReservationParticipant().getReservation();
        if (reservation.isComplete()) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CONSUMED);
        }
    }

    /* ReservationParticipantId에 대한 PaymentEntity가 이미 있고 상태가 READY나 PAID이면 같은 예약에 대해 중복 결제이기 때문에
       생성 시점에 확인 후 차단 */
    public void checkForDuplicatePaymentByParticipant(Integer participantId) {
        if (paymentRepository.existsByReservationParticipantIdAndStatusIn(participantId, List.of(PaymentStatus.READY, PaymentStatus.PAID))) {
            log.warn("[이미 진행중이거나 완료된 결제가 존재합니다] participantId={}", participantId);
            throw new AppException(ErrorCode.DUPLICATE_PAYMENT);
        }
    }
}
