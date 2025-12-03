package staysplit.hotel_reservation.hotel.dto.request;

public record CreateHotelRequest(
        String name,
        String address,
        Double longitude,
        Double latitude,
        String description,
        Integer starLevel
) {}
