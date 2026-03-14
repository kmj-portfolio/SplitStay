package staysplit.hotel_reservation.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationValidator {

    private final ReservationRepository reservationRepository;

    public ReservationEntity validateReservation(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.getMessage()));
    }

    public void validateReservationOwnership(Integer reservationId, String email) {
        boolean isOwner = reservationRepository.existsByReservationIdAndUserEmail(reservationId, email);
        if (!isOwner) {
            log.warn("[예약에 대한 권한이 없는 사용자] reservationId={}, userEmail={} ", reservationId, email);
            throw new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER, "해당 예약에 대한 권한이 없습니다.");
        }
    }
}
