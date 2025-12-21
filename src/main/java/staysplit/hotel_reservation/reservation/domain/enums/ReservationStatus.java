package staysplit.hotel_reservation.reservation.domain.enums;

import java.util.Optional;

public enum ReservationStatus {
    WAITING_PAYMENT, // 결제 미완료, 임시 예약 (재고 확보)
    CONFIRMED, // 결제 완료
    CANCELLED, // 예약 취소
    EXPIRED, // 시간 내 결제 미완료로 인한 취소
    COMPLETE; // 이용 완료

    public static Optional<ReservationStatus> from(String value) {
        try {
            return Optional.of(ReservationStatus.valueOf(value.toUpperCase()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
