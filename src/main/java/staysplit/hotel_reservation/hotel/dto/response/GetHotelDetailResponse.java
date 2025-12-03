package staysplit.hotel_reservation.hotel.dto.response;

import java.util.List;

public record GetHotelDetailResponse(
        Integer hotelId,
        String name,
        String address,
        Double longitude,
        Double latitude,
        String description,
        Integer starLevel,
        Double rating,
        Integer reviewCount,
        String mainPhotoUrl,
        List<String> additionalPhotoUrls
) {

}