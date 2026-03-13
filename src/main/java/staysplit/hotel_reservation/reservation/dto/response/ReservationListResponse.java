package staysplit.hotel_reservation.reservation.dto.response;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ReservationListResponse(
        Integer reservationId,
        String reservationNumber,
        String hotelName,
        String hotelMainImageUrl,
        String hotelAddress,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Long totalPrice,
        String reservationStatus,
        Integer numberOfParticipants
) {
}
