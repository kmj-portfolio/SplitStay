package staysplit.hotel_reservation.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.customer.domain.entity.CustomerEntity;
import staysplit.hotel_reservation.payment.domain.entity.PaymentEntity;
import staysplit.hotel_reservation.payment.repository.PaymentRepository;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;

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
}
