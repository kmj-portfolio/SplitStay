package staysplit.hotel_reservation.payment.domain.dto.response;

import lombok.Builder;
import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;

import java.time.LocalDateTime;

@Builder
public record PaymentResponse(
        String portOnePaymentId,
        Long paymentAmount,
        String payMethod,
        String cardPublisher,
        Integer reservationId,
        String reservationNumber,
        PaymentStatus status,
        LocalDateTime paidAt
) {
}
