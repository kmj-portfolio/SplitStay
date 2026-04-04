package staysplit.hotel_reservation.payment.sse.dto.response;

import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;

public record PaymentStatusResponse(
        String paymentId,
        PaymentStatus paymentStatus
) {
}
