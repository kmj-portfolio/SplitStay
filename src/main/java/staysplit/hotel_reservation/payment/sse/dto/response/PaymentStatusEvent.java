package staysplit.hotel_reservation.payment.sse.dto.response;

import staysplit.hotel_reservation.payment.domain.enums.PaymentStatus;

public record PaymentStatusEvent(
        String paymentId,
        PaymentStatus paymentStatus,
        String message
) {
    public static PaymentStatusEvent completed(String paymentId) {
        return new PaymentStatusEvent(paymentId, PaymentStatus.PAID, "결제가 완료되었습니다.");
    }

    public static PaymentStatusEvent failed(String paymentId) {
        return new PaymentStatusEvent(paymentId, PaymentStatus.FAILED, "결제에 실패했습니다.");
    }

    public static PaymentStatusEvent cancelled(String paymentId) {
        return new PaymentStatusEvent(paymentId, PaymentStatus.CANCELLED, "결제가 취소되었습니다.");
    }
}
