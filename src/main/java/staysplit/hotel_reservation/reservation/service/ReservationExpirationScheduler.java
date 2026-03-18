package staysplit.hotel_reservation.reservation.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import staysplit.hotel_reservation.reservation.domain.entity.ReservationEntity;
import staysplit.hotel_reservation.reservation.domain.enums.ReservationStatus;
import staysplit.hotel_reservation.reservation.reposiotry.ReservationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final ReservationRepository reservationRepository;

    @Transactional
    @Scheduled(fixedDelay = 60000) //1분 = 60s * 1000 ms
    public void expireWaitingReservations() {
        LocalDateTime now = LocalDateTime.now();

        List<ReservationEntity> expiredReservations =
                reservationRepository.findAllByStatusAndExpiresAtBefore(ReservationStatus.WAITING_PAYMENT, now);

        for (ReservationEntity reservation : expiredReservations) {
            reservation.markExpired();
        }
    }
}
