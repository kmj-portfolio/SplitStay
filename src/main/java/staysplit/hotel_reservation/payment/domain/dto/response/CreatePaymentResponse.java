package staysplit.hotel_reservation.payment.domain.dto.response;

public record CreatePaymentResponse(
        String paymentId,
        Long requiredPaymentAmount
) {}
