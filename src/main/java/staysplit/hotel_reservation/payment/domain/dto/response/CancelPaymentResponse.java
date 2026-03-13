package staysplit.hotel_reservation.payment.domain.dto.response;

public record CancelPaymentResponse(
   String portOnePaymentId,
   String reservationStatus,
   Long cancelledAmount
) {}
