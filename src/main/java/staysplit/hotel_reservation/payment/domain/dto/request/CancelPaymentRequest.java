package staysplit.hotel_reservation.payment.domain.dto.request;

public record CancelPaymentRequest(
        String paymentId,
        String reason
) {
}
