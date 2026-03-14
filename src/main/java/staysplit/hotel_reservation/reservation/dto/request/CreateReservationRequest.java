package staysplit.hotel_reservation.reservation.dto.request;


import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record CreateReservationRequest(
        Integer hotelId,
        List<RoomReservationRequest> roomsAndQuantities,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        List<String> nicknames,
        Boolean isSplitPayment

) {
}
