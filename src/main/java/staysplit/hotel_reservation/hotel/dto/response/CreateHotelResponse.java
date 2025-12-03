package staysplit.hotel_reservation.hotel.dto.response;

public record CreateHotelResponse(
        Integer hotelId,
        String name,
        String address,
        Double longitude,
        Double latitude,
        String description,
        Integer starLevel
) {
}