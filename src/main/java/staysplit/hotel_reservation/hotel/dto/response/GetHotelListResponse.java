package staysplit.hotel_reservation.hotel.dto.response;

public record GetHotelListResponse(
        Integer hotelId,
        String name,
        String address,
        Integer starLevel,
        Double rating,
        Integer reviewCount,
        String mainImageUrl
) {}
