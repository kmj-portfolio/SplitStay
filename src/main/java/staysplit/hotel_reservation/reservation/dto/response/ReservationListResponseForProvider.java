package staysplit.hotel_reservation.reservation.dto.response;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ReservationListResponseForProvider(
        Integer reservationId,
        String reservationNumber,
        Integer nights,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long totalPrice,
        String reservationStatus,
        List<String> participantNames,
        Integer numberOfParticipants
) {
}
