package staysplit.hotel_reservation.reservation.dto.response;

import lombok.Builder;

@Builder
public record ReservedRoomDetailResponse (
        Integer roomId,
        String roomType,
        Integer quantity,
        Integer pricePerNight,
        Integer nights,
        Long subTotal
) {
}
