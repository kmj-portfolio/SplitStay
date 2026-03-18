package staysplit.hotel_reservation.reservation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.common.exception.AppException;
import staysplit.hotel_reservation.common.exception.ErrorCode;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationParticipantEntity;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationParticipantRepository;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationValidator {

    private final ReservationRepository reservationRepository;
    private final ReservationParticipantRepository participantRepository;

    public ReservationEntity validateReservation(Integer reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESERVATION_NOT_FOUND, ErrorCode.RESERVATION_NOT_FOUND.getMessage()));
    }

    public ReservationParticipantEntity validateReservationParticipant(Integer customerId, Integer reservationId) {
        return participantRepository.findByCustomerIdAndReservationId(customerId, reservationId)
                .orElseThrow(() -> {
                    log.warn("[예약 참여자가 아님] customerId: {}, reservationId: {}", customerId, reservationId);
                    return new AppException(ErrorCode.UNAUTHORIZED_CUSTOMER);
                });
    }

    public void validateNotExpired(ReservationEntity reservationEntity) {
        boolean expired = reservationEntity.getExpiresAt().isBefore(LocalDateTime.now());
        if (expired) {
            throw new AppException(ErrorCode.EXPIRED_RESERVATION);
        }
    }
}
