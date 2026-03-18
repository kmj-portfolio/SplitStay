package staysplit.hotel_reservation.payment.domain.enums;

public enum PaymentStatus {
    READY,        // 결제 전 (예약만 생성)
    PAID,        // 결제 완료
    FAILED,      // 결제 실패
    CANCELLED,    // 결제 취소
}
